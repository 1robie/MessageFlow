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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern HEX_PATTERN = Pattern.compile("#([a-fA-F0-9]{6})");
    private static final Pattern MINI_MESSAGE_TAG_PATTERN = Pattern.compile("<[^>]+>");

    public LegacyMessageFormatter(@NotNull T plugin) {
        super(plugin);
    }

    @Override
    protected String load(@NotNull String msg) {
        String result = MINI_MESSAGE_TAG_PATTERN.matcher(msg).replaceAll("");
        Matcher matcher = HEX_PATTERN.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(
                    String.valueOf(ChatColor.of("#" + matcher.group(1)))
            ));
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
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
        if (this.textResolverRegistry.hasResolvers()) {
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

    public void sendMessage(
            @NotNull Message message,
            @NotNull Collection<? extends CommandSender> senders,
            boolean prefix,
            @NotNull Placeholder placeholders
    ) {
        for (MessageTypeAdapter messageAdapter : message.loaded()) {
            Collection<? extends CommandSender> resolvedSenders = this.resolveRecipients(messageAdapter, senders);
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
                        if (this.textResolverRegistry.hasResolvers()) {
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

                        if (this.textResolverRegistry.hasResolvers()) {
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
                        this.sendComponents(Bukkit.getOnlinePlayers(), sm, prefix, this.prefix, placeholders);
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
    public void playSound(@NotNull Collection<? extends Player> players, @NotNull SoundMessage soundMessage) {

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
                duration / 50L);
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
