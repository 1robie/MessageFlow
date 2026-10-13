package fr.robie.messageflow.formatter;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.api.PlaceholderValue;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.*;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message formatter implementation that uses Adventure's MiniMessage for modern text formatting.
 * <p>
 * This formatter supports hex colors, legacy color codes, and MiniMessage tags.
 * It is automatically selected when the Adventure API is available on the platform.
 *
 * @param <T> the type of the plugin using this formatter
 */
public class AdventureMessageFormatter<T extends Plugin> extends MessageFormatter<T, Component> {

    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern HEX_SHORT_PATTERN = Pattern.compile("(?<!<)(?<!:)(?<!</)#([a-fA-F0-9]{6})");
    private static final Pattern BARE_HEX_PATTERN = Pattern.compile("(?<![<&])(?<!:)(?<!</)#([A-Fa-f0-9]{6})");

    private static final Map<String, String> COLORS_MAPPINGS = Map.ofEntries(
            Map.entry("0", "black"),
            Map.entry("1", "dark_blue"),
            Map.entry("2", "dark_green"),
            Map.entry("3", "dark_aqua"),
            Map.entry("4", "dark_red"),
            Map.entry("5", "dark_purple"),
            Map.entry("6", "gold"),
            Map.entry("7", "gray"),
            Map.entry("8", "dark_gray"),
            Map.entry("9", "blue"),
            Map.entry("a", "green"),
            Map.entry("b", "aqua"),
            Map.entry("c", "red"),
            Map.entry("d", "light_purple"),
            Map.entry("e", "yellow"),
            Map.entry("f", "white"),
            Map.entry("k", "obfuscated"),
            Map.entry("l", "bold"),
            Map.entry("m", "strikethrough"),
            Map.entry("n", "underlined"),
            Map.entry("o", "italic"),
            Map.entry("r", "reset")
    );

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder().resolver(StandardTags.defaults()).build())
            .build();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();


    public AdventureMessageFormatter(@NotNull T plugin) {
        super(plugin);
    }

    /**
     * Placeholders become these tags before parsing, so their values are never read as MiniMessage.
     */
    private static final String PLACEHOLDER_TAG_PREFIX = "mfph_";
    private static final Pattern TAG_BREAKING = Pattern.compile("[<>'\"\\\\]");
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    @Override
    protected Component load(@NotNull String message) {
        return MINI_MESSAGE.deserialize(colorMiniMessage(message));
    }

    /**
     * Formats a message. Each local placeholder ({@code %key%} with a value in {@code placeholders}) is inserted
     * as a component after parsing: a value is plain text (or the given component) and can't add tags such as
     * {@code <click>}, colors or legacy codes. Player placeholders get {@code player}.
     * <p>
     * Inside a tag ({@code <click:run_command:'/tp %player%'>}) a placeholder is replaced by its text, without the
     * characters that could end the tag or its quotes ({@code < > ' " \}). Global placeholders and PlaceholderAPI
     * still return text that is parsed with the message.
     */
    @Override
    public @NotNull Component format(@Nullable String message, @Nullable Player player, @NotNull Placeholder placeholders) {
        if (message == null) {
            return this.empty();
        }
        if (placeholders.isEmpty()) {
            return super.format(message, player, placeholders);
        }

        TagResolver.Builder tags = TagResolver.builder();
        Map<String, String> tagNames = new HashMap<>();
        Matcher matcher = Placeholder.PLACEHOLDER_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        int tagDepth = 0;
        int scanned = 0;
        while (matcher.find()) {
            tagDepth = tagDepth(message, scanned, matcher.start(), tagDepth);
            scanned = matcher.start();
            String token = matcher.group();
            String key = token.substring(1, token.length() - 1);
            PlaceholderValue value = placeholders.get(key);
            if (value == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(token));
                continue;
            }
            if (tagDepth > 0) {
                String text = PLAIN_SERIALIZER.serialize(toComponent(token, value, player));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(TAG_BREAKING.matcher(text).replaceAll("")));
                continue;
            }
            String tagName = tagNames.get(key);
            if (tagName == null) {
                tagName = PLACEHOLDER_TAG_PREFIX + tagNames.size();
                tagNames.put(key, tagName);
                tags.resolver(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(tagName, toComponent(token, value, player)));
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement("<" + tagName + ">"));
        }
        matcher.appendTail(sb);

        String text = this.applyResolvers(sb.toString(), player, placeholders);
        return MINI_MESSAGE.deserialize(colorMiniMessage(text), tags.build());
    }

    /**
     * Follows {@code <} and {@code >} (not escaped by a backslash) from {@code from} to {@code to}.
     */
    private static int tagDepth(@NotNull String message, int from, int to, int depth) {
        for (int i = from; i < to; i++) {
            char c = message.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '<') {
                depth++;
            } else if (c == '>' && depth > 0) {
                depth--;
            }
        }
        return depth;
    }

    private static @NotNull Component toComponent(@NotNull String token, @NotNull PlaceholderValue value, @Nullable Player player) {
        if (value instanceof PlaceholderValue.ComponentValue(Component component)) {
            return component;
        }
        try {
            return Component.text(value.evaluate(player));
        } catch (Exception e) {
            return Component.text(token);
        }
    }

    @Override
    protected @NotNull Component join(@NotNull List<Component> lines) {
        return lines.size() == 1 ? lines.getFirst() : Component.join(JoinConfiguration.newlines(), lines);
    }

    @NotNull
    public Component getComponent(@Nullable String message) {
        return this.getComponent(message, null, Placeholder.empty());
    }

    @NotNull
    public Component getComponent(@Nullable String message, @Nullable Player player) {
        return this.getComponent(message, player, Placeholder.empty());
    }

    @NotNull
    public Component getComponent(@Nullable String message, @NotNull Placeholder placeholders) {
        return this.getComponent(message, null, placeholders);
    }

    @NotNull
    public Component getComponent(@Nullable String message, @Nullable Player player, @NotNull Placeholder placeholders) {
        if (message == null || message.isBlank()) {
            return this.empty();
        }
        return this.format(message, player, placeholders);
    }

    @NotNull
    public String serialize(@NotNull Component component) {
        return MINI_MESSAGE.serialize(component);
    }

    @Override
    public @NotNull String getLegacyColoredMessage(@NotNull String message, @Nullable Player player, @NotNull Placeholder placeholders) {
        return LEGACY_SERIALIZER.serialize(this.format(message, player, placeholders));
    }

    @Override
    protected @NonNull Component empty() {
        return Component.empty();
    }

    private static String colorMiniMessage(@NotNull String message) {
        String result = convertLegacyHex(message);
        result = convertShortLegacyHex(result);
        result = BARE_HEX_PATTERN.matcher(result).replaceAll("<#$1>");
        result = replaceLegacyColors(result);
        return result;
    }

    private static @NotNull String convertLegacyHex(@NotNull String message) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group().replaceAll("§x|§", "");
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static @NotNull String convertShortLegacyHex(@NotNull String message) {
        Matcher matcher = HEX_SHORT_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String replaceLegacyColors(@NotNull String message) {
        for (var entry : COLORS_MAPPINGS.entrySet()) {
            String key = entry.getKey();
            String value = "<" + entry.getValue() + ">";
            message = message
                    .replace("&" + key, value)
                    .replace("§" + key, value)
                    .replace("&" + key.toUpperCase(Locale.ROOT), value)
                    .replace("§" + key.toUpperCase(Locale.ROOT), value);
        }
        return message;
    }


    private void sendComponents(
            @NotNull Collection<? extends Audience> audiences,
            @NotNull SimpleMessage message,
            boolean enablePrefix,
            @Nullable String prefix,
            @NotNull Placeholder placeholders,
            @NotNull BiConsumer<Audience, Component> sendAction
    ) {
        List<String> messages = message.messages();
        if (messages.isEmpty() || messages.stream().allMatch(s -> s == null || s.isBlank())) {
            return;
        }
        this.perAudienceOrShared(audiences, messages, placeholders, enablePrefix && prefix != null ? prefix : null, sendAction);
    }

    @Override
    public void sendTitle(
            @NotNull Collection<? extends @NotNull Player> players,
            @Nullable String title, @Nullable String subtitle,
            int fadeIn, int stay, int fadeOut,
            @NotNull Placeholder placeholders
    ) {
        if (players.isEmpty()) {
            return;
        }

        if (this.textResolverRegistry.hasResolvers()) {
            players.forEach(player ->
                    player.showTitle(this.buildTitle(title, subtitle, fadeIn, stay, fadeOut, player, placeholders)));
        } else {
            Title shared = this.buildTitle(title, subtitle, fadeIn, stay, fadeOut, null, placeholders);
            players.forEach(player -> player.showTitle(shared));
        }
    }

    @Override
    public void sendActionBar(
            @NotNull Collection<? extends @NotNull Player> players,
            @Nullable String message,
            @NotNull Placeholder placeholders
    ) {
        this.sendActionBar(players, message, false, placeholders);
    }

    @Override
    public void sendActionBar(
            @NotNull Collection<? extends @NotNull Player> players,
            @Nullable String message,
            boolean prefix,
            @NotNull Placeholder placeholders
    ) {
        if (message == null || players.isEmpty()) {
            return;
        }
        String text = (prefix ? this.prefix : "") + message;
        this.perAudienceOrShared(players, text, placeholders, Player::sendActionBar);
    }

    @Override
    public void broadcastActionBar(@Nullable String message, boolean prefix, @NotNull Placeholder placeholders) {
        this.sendActionBar(Bukkit.getOnlinePlayers(), message, prefix, placeholders);
    }

    @Override
    public void sendMessage(
            @NotNull Collection<? extends CommandSender> senders,
            @Nullable String message,
            boolean prefix,
            @NotNull Placeholder placeholders
    ) {
        if (message == null || senders.isEmpty()) {
            return;
        }
        String text = (prefix ? this.prefix : "") + message;
        this.perAudienceOrShared(senders, text, placeholders, CommandSender::sendMessage);
    }

    @Override
    public void broadcast(@Nullable String message, boolean prefix, @NotNull Placeholder placeholders) {
        this.sendMessage(Bukkit.getOnlinePlayers(), message, prefix, placeholders);
    }

    public void sendMessage(
            @NotNull Message message,
            @NotNull Collection<? extends CommandSender> audiences,
            boolean prefix,
            @NotNull Placeholder placeholders
    ) {
        for (MessageTypeAdapter messageAdapter : message.loaded()) {
            Collection<? extends CommandSender> resolvedAudiences = this.resolveRecipients(messageAdapter, audiences);
            if (resolvedAudiences.isEmpty()) {
                continue;
            }

            switch (messageAdapter.messageType()) {

                case TITLE -> {
                    if (messageAdapter instanceof TitleMessage tm) {
                        String title = tm.title();
                        String subtitle = tm.subtitle();
                        int fadeIn = tm.fadeIn();
                        int stay = tm.stay();
                        int fadeOut = tm.fadeOut();
                        if (this.textResolverRegistry.hasResolvers()) {
                            resolvedAudiences.forEach(a -> {
                                Player player = a instanceof Player p ? p : null;
                                a.showTitle(this.buildTitle(title, subtitle, fadeIn, stay, fadeOut, player, placeholders));
                            });
                        } else {
                            Title shared = this.buildTitle(title, subtitle, fadeIn, stay, fadeOut, null, placeholders);
                            resolvedAudiences.forEach(a -> a.showTitle(shared));
                        }
                    }
                }

                case TCHAT -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(resolvedAudiences, sm, prefix, this.prefix, placeholders, Audience::sendMessage);
                    }
                }

                case ACTION_BAR -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(resolvedAudiences, sm, prefix, this.prefix, placeholders, Audience::sendActionBar);
                    }
                }

                case BOSS_BAR -> {
                    if (messageAdapter instanceof AdventureBossBarMessage abm) {
                        String title = abm.title();
                        BossBar.Color color = abm.color();
                        BossBar.Overlay overlay = abm.overlay();
                        Set<BossBar.Flag> flags = abm.flags();
                        long duration = abm.duration();
                        float progress = abm.progress();

                        if (this.textResolverRegistry.hasResolvers()) {
                            resolvedAudiences.forEach(a -> {
                                Player player = a instanceof Player p ? p : null;
                                BossBar bar = BossBar.bossBar(
                                        this.format(title, player, placeholders),
                                        progress, color, overlay, flags);
                                a.showBossBar(bar);
                                this.scheduleHideBossBar(duration, a, Audience::hideBossBar, bar);
                            });
                        } else {
                            BossBar bar = BossBar.bossBar(
                                    this.format(title, null, placeholders),
                                    progress, color, overlay, flags);
                            resolvedAudiences.forEach(a -> a.showBossBar(bar));
                            this.scheduleHideBossBar(duration, resolvedAudiences, Audience::hideBossBar, bar);
                        }
                    }
                }

                case WITHOUT_PREFIX -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(resolvedAudiences, sm, false, null, placeholders, Audience::sendMessage);
                    }
                }

                case BROADCAST -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        Collection<? extends CommandSender> everyone = this.resolveRecipients(true, sm.sendToConsole(), sm.excludeSenders(), audiences);
                        this.sendComponents(everyone, sm, prefix, this.prefix, placeholders, Audience::sendMessage);
                    }
                }

                case SOUND -> {
                    if (messageAdapter instanceof SoundMessage soundMessage) {
                        this.playSound(resolvedAudiences.stream().filter(a -> a instanceof Player).map(a -> (Player) a).toList(), soundMessage);
                    }
                }
            }
        }
    }

    @Override
    public void sendMessage(
            @NotNull Message message,
            @NotNull Logger.LogType logType,
            @NotNull ConsoleCommandSender sender,
            @NotNull Placeholder placeholders
    ) {
        String logPrefix = Logger.getPrefix(logType);
        for (MessageTypeAdapter adapter : message.loaded()) {
            if (adapter.messageType() == MessageType.TCHAT && adapter instanceof SimpleMessage sm) {
                this.sendComponents(Collections.singleton(sender), sm, true, logPrefix, placeholders, Audience::sendMessage);
            }
        }
    }


    // -----
    // Utility methods
    // -----

    private Title buildTitle(
            @Nullable String titleText,
            @Nullable String subtitleText,
            int fadeIn, int stay, int fadeOut,
            @Nullable Player player,
            @NotNull Placeholder placeholders
    ) {
        return Title.title(
                this.format(titleText, player, placeholders),
                this.format(subtitleText, player, placeholders),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );
    }

}
