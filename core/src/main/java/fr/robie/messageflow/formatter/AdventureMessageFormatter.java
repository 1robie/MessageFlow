package fr.robie.messageflow.formatter;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.api.PlaceholderValue;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.*;
import fr.robie.messageflow.util.PlaceholderScanner;
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
import java.util.function.Predicate;

/**
 * Message formatter implementation that uses Adventure's MiniMessage for modern text formatting.
 * <p>
 * This formatter supports hex colors, legacy color codes, and MiniMessage tags.
 * It is automatically selected when the Adventure API is available on the platform.
 *
 * @param <T> the type of the plugin using this formatter
 */
public class AdventureMessageFormatter<T extends Plugin> extends MessageFormatter<T, Component> {

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

        PlaceholderTags tags = new PlaceholderTags(message, player, placeholders);
        String text = PlaceholderScanner.replace(message, tags);
        text = this.applyResolvers(text, player, placeholders);
        return MINI_MESSAGE.deserialize(colorMiniMessage(text), tags.build());
    }

    /**
     * Turns each local placeholder into a {@code <mfph_N>} tag (or into its tag-safe text inside a tag).
     */
    private static final class PlaceholderTags implements PlaceholderScanner.Resolver {
        private final String message;
        private final Player player;
        private final Placeholder placeholders;
        private final TagResolver.Builder tags = TagResolver.builder();
        private Map<String, String> tagNames;
        private int tagDepth;
        private int scanned;

        PlaceholderTags(@NotNull String message, @Nullable Player player, @NotNull Placeholder placeholders) {
            this.message = message;
            this.player = player;
            this.placeholders = placeholders;
        }

        @Override
        public @Nullable String resolve(@NotNull String key, int start) {
            this.tagDepth = tagDepth(this.message, this.scanned, start, this.tagDepth);
            this.scanned = start;
            PlaceholderValue value = this.placeholders.get(key);
            if (value == null) {
                return null;
            }
            if (this.tagDepth > 0) {
                return stripTagBreaking(PLAIN_SERIALIZER.serialize(toComponent(key, value, this.player)));
            }
            if (this.tagNames == null) {
                this.tagNames = new HashMap<>(4);
            }
            String tagName = this.tagNames.get(key);
            if (tagName == null) {
                tagName = PLACEHOLDER_TAG_PREFIX + this.tagNames.size();
                this.tagNames.put(key, tagName);
                this.tags.resolver(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(tagName, toComponent(key, value, this.player)));
            }
            return "<" + tagName + ">";
        }

        @NotNull TagResolver build() {
            return this.tags.build();
        }
    }

    /**
     * Removes the characters that could end a tag or its quotes ({@code < > ' " \}).
     */
    private static @NotNull String stripTagBreaking(@NotNull String text) {
        StringBuilder out = null;
        for (int i = 0, length = text.length(); i < length; i++) {
            char c = text.charAt(i);
            boolean breaking = c == '<' || c == '>' || c == '\'' || c == '"' || c == '\\';
            if (breaking && out == null) {
                out = new StringBuilder(length).append(text, 0, i);
            } else if (!breaking && out != null) {
                out.append(c);
            }
        }
        return out != null ? out.toString() : text;
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

    private static @NotNull Component toComponent(@NotNull String key, @NotNull PlaceholderValue value, @Nullable Player player) {
        if (value instanceof PlaceholderValue.ComponentValue(Component component)) {
            return component;
        }
        try {
            return Component.text(value.evaluate(player));
        } catch (Exception e) {
            return Component.text("%" + key + "%");
        }
    }

    @Override
    protected @NotNull String plain(@NotNull Component formatted) {
        return PLAIN_SERIALIZER.serialize(formatted);
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

    /**
     * Converts legacy colors to MiniMessage tags in one pass: {@code §x§r§r§g§g§b§b}, {@code &#rrggbb} and a bare
     * {@code #rrggbb} become {@code <#rrggbb>}, and {@code &c} / {@code §c} become {@code <red>}. A {@code #rrggbb}
     * right after {@code <}, {@code :} or {@code </} is already part of a tag and is kept.
     */
    static @NotNull String colorMiniMessage(@NotNull String message) {
        int length = message.length();
        int i = 0;
        while (i < length) {
            char c = message.charAt(i);
            if (c == '&' || c == '§' || c == '#') {
                break;
            }
            i++;
        }
        if (i == length) {
            return message;
        }

        StringBuilder out = new StringBuilder(length + 16).append(message, 0, i);
        while (i < length) {
            char c = message.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < length) {
                char code = message.charAt(i + 1);
                if (c == '§' && code == 'x' && isSectionHex(message, i + 2)) {
                    out.append("<#");
                    for (int d = i + 3; d < i + 14; d += 2) {
                        out.append(message.charAt(d));
                    }
                    out.append('>');
                    i += 14;
                    continue;
                }
                if (c == '&' && code == '#' && isHex(message, i + 2)) {
                    out.append('<').append(message, i + 1, i + 8).append('>');
                    i += 8;
                    continue;
                }
                String name = colorName(code);
                if (name != null) {
                    out.append('<').append(name).append('>');
                    i += 2;
                    continue;
                }
            } else if (c == '#' && isHex(message, i + 1) && !afterTagStart(out)) {
                out.append('<').append(message, i, i + 7).append('>');
                i += 7;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean afterTagStart(@NotNull StringBuilder out) {
        int n = out.length();
        if (n == 0) {
            return false;
        }
        char previous = out.charAt(n - 1);
        return previous == '<' || previous == ':' || (previous == '/' && n > 1 && out.charAt(n - 2) == '<');
    }

    /**
     * Six hex digits from {@code from}.
     */
    private static boolean isHex(@NotNull String s, int from) {
        if (from + 6 > s.length()) {
            return false;
        }
        for (int i = from; i < from + 6; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Six {@code §} + hex digit pairs from {@code from}.
     */
    private static boolean isSectionHex(@NotNull String s, int from) {
        if (from + 12 > s.length()) {
            return false;
        }
        for (int i = from; i < from + 12; i += 2) {
            if (s.charAt(i) != '§' || Character.digit(s.charAt(i + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String colorName(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
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

        if (this.isPlayerDependent(title, placeholders) || this.isPlayerDependent(subtitle, placeholders)) {
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

    @Override
    protected void dispatch(
            @NotNull List<? extends MessageTypeAdapter> adapters,
            @NotNull Collection<? extends CommandSender> audiences,
            @Nullable Predicate<CommandSender> recipients,
            boolean prefix,
            @NotNull Placeholder placeholders
    ) {
        for (MessageTypeAdapter messageAdapter : adapters) {
            Collection<? extends CommandSender> resolvedAudiences = keep(this.resolveRecipients(messageAdapter, audiences), recipients);
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
                        if (this.isPlayerDependent(title, placeholders) || this.isPlayerDependent(subtitle, placeholders)) {
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

                        if (this.isPlayerDependent(title, placeholders)) {
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
                        Collection<? extends CommandSender> everyone = keep(this.resolveRecipients(true, sm.sendToConsole(), sm.excludeSenders(), audiences), recipients);
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
