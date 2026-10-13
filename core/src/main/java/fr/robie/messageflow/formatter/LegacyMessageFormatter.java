package fr.robie.messageflow.formatter;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Message formatter implementation that uses legacy Bukkit color codes for text formatting.
 * <p>
 * This formatter supports hex colors (converted to legacy format) and ampersand color codes.
 * It is automatically selected when the Adventure API is not available on the platform.
 *
 * @param <T> the type of the plugin using this formatter
 */
@SuppressWarnings("deprecation")
public class LegacyMessageFormatter<T extends Plugin> extends MessageFormatter<T, String> {

    private static final String LEGACY_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    public LegacyMessageFormatter(@NotNull T plugin) {
        super(plugin);
    }

    /**
     * Removes MiniMessage tags, then turns {@code #rrggbb} (or {@code &#rrggbb}) into {@code §x§r§r§g§g§b§b}
     * and {@code &c} into {@code §c}.
     */
    @Override
    protected String load(@NotNull String msg) {
        String text = stripTags(msg);
        int length = text.length();
        int i = 0;
        while (i < length && text.charAt(i) != '&' && text.charAt(i) != '#') {
            i++;
        }
        if (i == length) {
            return text;
        }

        StringBuilder out = new StringBuilder(length + 32).append(text, 0, i);
        while (i < length) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < length) {
                char code = text.charAt(i + 1);
                if (code == '#' && isHex(text, i + 2)) {
                    appendHex(out, text, i + 2);
                    i += 8;
                    continue;
                }
                if (LEGACY_CODES.indexOf(code) >= 0) {
                    out.append(ChatColor.COLOR_CHAR).append(Character.toLowerCase(code));
                    i += 2;
                    continue;
                }
            } else if (c == '#' && isHex(text, i + 1)) {
                appendHex(out, text, i + 1);
                i += 7;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Removes every {@code <...>} with at least one character inside.
     */
    private static @NotNull String stripTags(@NotNull String text) {
        int open = text.indexOf('<');
        if (open < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int copied = 0;
        while (open >= 0) {
            int close = text.indexOf('>', open + 1);
            if (close < 0) {
                break;
            }
            if (close == open + 1) {
                open = text.indexOf('<', close);
                continue;
            }
            out.append(text, copied, open);
            copied = close + 1;
            open = text.indexOf('<', copied);
        }
        return out.append(text, copied, text.length()).toString();
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
     * Appends the six hex digits from {@code from} as {@code §x§r§r§g§g§b§b}, like {@code ChatColor.of("#rrggbb")}.
     */
    private static void appendHex(@NotNull StringBuilder out, @NotNull String s, int from) {
        out.append(ChatColor.COLOR_CHAR).append('x');
        for (int i = from; i < from + 6; i++) {
            out.append(ChatColor.COLOR_CHAR).append(s.charAt(i));
        }
    }

    @Override
    public @NotNull String getLegacyColoredMessage(@NotNull String message, @Nullable Player player, @NotNull Placeholder placeholders) {
        return this.format(message, player, placeholders);
    }

    @Override
    protected @NotNull String plain(@NotNull String formatted) {
        String stripped = org.bukkit.ChatColor.stripColor(formatted);
        return stripped != null ? stripped : "";
    }

    @Override
    protected @NotNull String join(@NotNull List<String> lines) {
        return String.join("\n", lines);
    }

    @Override
    protected @NonNull String empty() {
        return "";
    }

    private void sendComponents(
            @NotNull Collection<? extends CommandSender> senders,
            @NotNull SimpleMessage message,
            boolean enablePrefix,
            @Nullable String prefix,
            @NotNull Placeholder placeholders
    ) {
        List<String> messages = message.messages();
        if (messages.isEmpty() || messages.stream().allMatch(s -> s == null || s.isBlank())) {
            return;
        }

        this.perAudienceOrShared(senders, messages, placeholders, enablePrefix && prefix != null ? prefix : null, CommandSender::sendMessage);
    }

    @Override
    public void sendTitle(
            @NotNull Collection<? extends @NotNull Player> players,
            @Nullable String title, @Nullable String subtitle,
            int fadeIn, int stay, int fadeOut,
            @NotNull Placeholder placeholders
    ) {
        if (this.isPlayerDependent(title, placeholders) || this.isPlayerDependent(subtitle, placeholders)) {
            players.forEach(player -> player.sendTitle(
                    this.format(title, player, placeholders),
                    this.format(subtitle, player, placeholders),
                    fadeIn, stay, fadeOut
            ));
        } else {
            String sharedTitle = this.format(title, null, placeholders);
            String sharedSubtitle = this.format(subtitle, null, placeholders);
            players.forEach(player -> player.sendTitle(sharedTitle, sharedSubtitle, fadeIn, stay, fadeOut));
        }
    }

    @Override
    public void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, @NotNull Placeholder placeholders) {
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
            @NotNull Collection<? extends CommandSender> senders,
            @Nullable Predicate<CommandSender> recipients,
            boolean prefix,
            @NotNull Placeholder placeholders
    ) {
        for (MessageTypeAdapter messageAdapter : adapters) {
            Collection<? extends CommandSender> resolvedSenders = keep(this.resolveRecipients(messageAdapter, senders), recipients);
            if (resolvedSenders.isEmpty()) {
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
                            resolvedSenders.forEach(sender -> {
                                Player player = sender instanceof Player p ? p : null;
                                String coloredTitle = this.format(title, player, placeholders);
                                String coloredSubtitle = this.format(subtitle, player, placeholders);
                                if (sender instanceof Player p) {
                                    p.sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
                                } else {
                                    sender.sendMessage(coloredTitle);
                                    sender.sendMessage(coloredSubtitle);
                                }
                            });
                        } else {
                            String sharedTitle = this.format(title, null, placeholders);
                            String sharedSubtitle = this.format(subtitle, null, placeholders);
                            resolvedSenders.forEach(sender -> {
                                if (sender instanceof Player p) {
                                    p.sendTitle(sharedTitle, sharedSubtitle, fadeIn, stay, fadeOut);
                                } else {
                                    sender.sendMessage(sharedTitle);
                                    sender.sendMessage(sharedSubtitle);
                                }
                            });
                        }
                    }
                }

                case TCHAT -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(resolvedSenders, sm, prefix, this.prefix, placeholders);
                    }
                }

                case ACTION_BAR -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        String line = sm.messages().isEmpty() ? null : sm.messages().getFirst();
                        List<? extends Player> players = resolvedSenders.stream()
                                .filter(s -> s instanceof Player)
                                .map(s -> (Player) s)
                                .toList();
                        this.sendActionBar(players, line, prefix, placeholders);
                    }
                }

                case BOSS_BAR -> {
                    if (messageAdapter instanceof LegacyBossBarMessage lbm) {
                        String title = lbm.title();
                        BarColor color = lbm.color();
                        BarStyle style = lbm.style();
                        BarFlag[] flags = lbm.flags();
                        long duration = lbm.duration();
                        float progress = lbm.progress();

                        List<Player> players = resolvedSenders.stream()
                                .filter(s -> s instanceof Player)
                                .map(s -> (Player) s)
                                .toList();

                        if (this.isPlayerDependent(title, placeholders)) {
                            players.forEach(p -> {
                                BossBar bar = this.createBossBar(
                                        this.format(title, p, placeholders),
                                        color, style, flags, progress);
                                this.showBossBar(bar, p, duration);
                            });
                        } else {
                            String sharedTitle = this.format(title, null, placeholders);
                            BossBar bar = this.createBossBar(sharedTitle, color, style, flags, progress);


                            players.forEach(p -> this.showBossBar(bar, p, duration));
                        }
                    }
                }

                case WITHOUT_PREFIX -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        this.sendComponents(resolvedSenders, sm, false, null, placeholders);
                    }
                }

                case BROADCAST -> {
                    if (messageAdapter instanceof SimpleMessage sm) {
                        Collection<? extends CommandSender> everyone = keep(this.resolveRecipients(true, sm.sendToConsole(), sm.excludeSenders(), senders), recipients);
                        this.sendComponents(everyone, sm, prefix, this.prefix, placeholders);
                    }
                }

                case SOUND -> {
                    if (messageAdapter instanceof SoundMessage soundMessage) {
                        this.playSound(resolvedSenders.stream()
                                .filter(s -> s instanceof Player)
                                .map(s -> (Player) s)
                                .toList(), soundMessage);
                    }
                }
            }
        }
    }


    @Override
    public void sendMessage(@NotNull Message message, @NotNull Logger.LogType logType, @NotNull ConsoleCommandSender sender, @NotNull Placeholder placeholders) {
        String prefix = Logger.getPrefix(logType);
        for (MessageTypeAdapter messageAdapter : message.loaded()) {
            if (messageAdapter.messageType() == MessageType.TCHAT && messageAdapter instanceof SimpleMessage simpleMessage) {
                this.sendComponents(Collections.singleton(sender), simpleMessage, true, prefix, placeholders);
            }
        }
    }

    // ---
    // Utility methods
    // ---

    private void showBossBar(@NotNull BossBar bar, @NotNull Player player, long duration) {
        bar.addPlayer(player);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                () -> {
                    bar.removePlayer(player);
                    bar.setVisible(false);
                },
                duration);
    }

    private @NotNull BossBar createBossBar(
            @NotNull String title,
            @NotNull BarColor color,
            @NotNull BarStyle style,
            @Nullable BarFlag[] flags,
            float progress
    ) {
        BossBar bar = (flags != null && flags.length > 0)
                ? Bukkit.createBossBar(title, color, style, flags)
                : Bukkit.createBossBar(title, color, style);
        bar.setProgress(Math.clamp(progress, 0f, 1f));
        return bar;
    }

}
