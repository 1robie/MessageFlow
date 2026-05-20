package fr.robie.messageflow.formatter;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.*;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
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
import java.util.concurrent.TimeUnit;
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


    public AdventureMessageFormatter(@NotNull T plugin, @NotNull ConfigurationOptions<?> options) {
        super(plugin, options);
    }

    @Override
    protected Component load(@NotNull String message) {
        return MINI_MESSAGE.deserialize(colorMiniMessage(message));
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
                    .replace("&" + key.toUpperCase(), value)
                    .replace("§" + key.toUpperCase(), value);
        }
        return message;
    }


    private void sendComponents(
            @NotNull Collection<? extends Audience> audiences,
            @NotNull SimpleMessage message,
            boolean enablePrefix,
            @Nullable String prefix,
            @NotNull Object[] placeholders,
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
            @NonNull @NotNull Object... placeholders
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
            @NonNull @NotNull Object... placeholders
    ) {
        this.sendActionBar(players, message, false, placeholders);
    }

    @Override
    public void sendActionBar(
            @NotNull Collection<? extends @NotNull Player> players,
            @Nullable String message,
            boolean prefix,
            @NotNull Object... placeholders
    ) {
        if (message == null || players.isEmpty()) {
            return;
        }
        String text = (prefix ? this.prefix : "") + message;
        this.perAudienceOrShared(players, text, placeholders, Player::sendActionBar);
    }

    @Override
    public void broadcastActionBar(@Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        this.sendActionBar(Bukkit.getOnlinePlayers(), message, prefix, placeholders);
    }

    @Override
    public void sendMessage(
            @NotNull Collection<? extends CommandSender> senders,
            @Nullable String message,
            boolean prefix,
            @NotNull Object... placeholders
    ) {
        if (message == null || senders.isEmpty()) {
            return;
        }
        String text = (prefix ? this.prefix : "") + message;
        this.perAudienceOrShared(senders, text, placeholders, CommandSender::sendMessage);
    }

    @Override
    public void broadcast(@Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        this.sendMessage(Bukkit.getOnlinePlayers(), message, prefix, placeholders);
    }

    public void sendMessage(
            @NotNull Message message,
            @NotNull Collection<? extends CommandSender> audiences,
            boolean prefix,
            @NotNull Object... placeholders
    ) {
        if (audiences.isEmpty()) {
            return;
        }

        for (MessageTypeAdapter messageAdapter : message.loaded()) {
            switch (messageAdapter.messageType()) {

                case TITLE -> {
                    if (messageAdapter instanceof TitleMessage(
                            String title, String subtitle, int fadeIn, int stay, int fadeOut
                    )) {
                        if (this.textResolverRegistry.hasResolvers()) {
                            audiences.forEach(a -> {
                                Player player = a instanceof Player p ? p : null;
                                a.showTitle(this.buildTitle(title, subtitle, fadeIn, stay, fadeOut, player, placeholders));
                            });
                        } else {
                            Title shared = this.buildTitle(title, subtitle, fadeIn, stay, fadeOut, null, placeholders);
                            audiences.forEach(a -> a.showTitle(shared));
                        }
                    }
                }

                case TCHAT -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(audiences, sm, prefix, this.prefix, placeholders, Audience::sendMessage);
                    }
                }

                case ACTION_BAR -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(audiences, sm, prefix, this.prefix, placeholders, Audience::sendActionBar);
                    }
                }

                case BOSS_BAR -> {
                    if (messageAdapter instanceof AdventureBossBarMessage(
                            String title, BossBar.Color color, BossBar.Overlay overlay,
                            Set<BossBar.Flag> flags, long duration, float progress
                    )) {
                        if (this.textResolverRegistry.hasResolvers()) {
                            audiences.forEach(a -> {
                                Player player = a instanceof Player p ? p : null;
                                BossBar bar = BossBar.bossBar(
                                        this.format(title, player, placeholders),
                                        progress, color, overlay, flags);
                                a.showBossBar(bar);
                                this.scheduleHideBossBar(bar, duration, Collections.singleton(a));
                            });
                        } else {
                            BossBar bar = BossBar.bossBar(
                                    this.format(title, null, placeholders),
                                    progress, color, overlay, flags);
                            audiences.forEach(a -> a.showBossBar(bar));
                            this.scheduleHideBossBar(bar, duration, audiences);
                        }
                    }
                }

                case WITHOUT_PREFIX -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(audiences, sm, false, null, placeholders, Audience::sendMessage);
                    }
                }

                case BROADCAST -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(Bukkit.getOnlinePlayers(), sm, prefix, this.prefix, placeholders, Audience::sendMessage);
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
            @NotNull Object... placeholders
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
            @NotNull Object[] placeholders
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

    private void scheduleHideBossBar(
            @NotNull BossBar bar,
            long durationTicks,
            @NotNull Collection<? extends Audience> audiences
    ) {
        this.plugin.getServer().getAsyncScheduler().runDelayed(
                this.plugin,
                w -> audiences.forEach(a -> a.hideBossBar(bar)),
                durationTicks * 50L,
                TimeUnit.MILLISECONDS
        );
    }
}
