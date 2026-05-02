package fr.robie.messageflow.format;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import fr.robie.messageflow.Message;
import fr.robie.messageflow.MessageTypeAdapter;
import fr.robie.messageflow.message.LegacyBossBarMessage;
import fr.robie.messageflow.message.SimpleMessage;
import fr.robie.messageflow.message.TitleMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class LegacyMessageFormatter<T extends Plugin> extends MessageFormatter<T> {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([a-fA-F0-9]{6})");
    private static final Pattern MINI_MESSAGE_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final com.google.common.cache.LoadingCache<String, String> COLORIZE_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(512)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build(CacheLoader.from(msg -> {
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
                    }));

    public LegacyMessageFormatter(@NotNull T plugin) {
        super(plugin);
    }

    public @NotNull String colorize(@NotNull String message) {
        return COLORIZE_CACHE.getUnchecked(message);
    }

    private @NotNull String colorizeWithPlaceholders(@Nullable String message, @NotNull Object... placeholders) {
        if (message == null) {
            return "";
        }
        if (placeholders.length == 0) {
            return this.colorize(message);
        }
        return this.colorize(this.parseText(message, placeholders));
    }

    private List<String> getLines(@NotNull SimpleMessage message, boolean prefix, @NotNull Object[] placeholders) {
        List<String> messages = message.messages();
        if (messages.isEmpty() || messages.stream().allMatch(s -> s == null || s.isBlank())) {
            return Collections.emptyList();
        }

        String prefixText = prefix ? this.prefix : "";

        return messages.stream()
                .map(s -> s == null ? "" : this.colorizeWithPlaceholders(prefixText + s, placeholders))
                .toList();
    }

    private void sendLines(@NotNull Collection<CommandSender> senders, @NotNull List<String> lines) {
        senders.forEach(sender -> lines.forEach(sender::sendMessage));
    }

    private void sendComponents(
            @NotNull Collection<CommandSender> senders,
            @NotNull SimpleMessage message,
            boolean prefix,
            @NotNull Object[] placeholders
    ) {
        List<String> lines = this.getLines(message, prefix, placeholders);
        if (!lines.isEmpty()) {
            this.sendLines(senders, lines);
        }
    }

    @Override
    public void sendTitle(@NotNull Collection<@NotNull Player> players, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NonNull @NotNull Object... placeholders) {
        String parsedTitle = this.colorizeWithPlaceholders(title, placeholders);
        String parsedSubtitle = this.colorizeWithPlaceholders(subtitle, placeholders);
        players.forEach(player -> player.sendTitle(parsedTitle, parsedSubtitle, fadeIn, stay, fadeOut));
    }

    @Override
    public void sendActionBar(@NotNull Collection<@NotNull Player> players, @Nullable String message, @NonNull @NotNull Object... placeholders) {
        this.sendActionBar(players, message, false, placeholders);
    }

    @Override
    public void sendActionBar(@NotNull Collection<@NotNull Player> players, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (message == null || players.isEmpty()) {
            return;
        }

        String prefixText = prefix ? this.prefix : "";
        String colorized = this.colorizeWithPlaceholders(prefixText + message, placeholders);
        players.forEach(player -> player.sendActionBar(colorized));
    }

    @Override
    public void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (message == null || senders.isEmpty()) {
            return;
        }

        String prefixText = prefix ? this.prefix : "";
        String colorized = this.colorizeWithPlaceholders(prefixText + message, placeholders);

        senders.forEach(sender -> sender.sendMessage(colorized));
    }

    @Override
    public void broadcast(@Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        this.sendMessage(Bukkit.getOnlinePlayers(), message, prefix, placeholders);
    }

    public void sendMessage(
            @NotNull Message message,
            @NotNull Collection<CommandSender> senders,
            boolean prefix,
            @NotNull Object... placeholders
    ) {
        if (senders.isEmpty()) {
            return;
        }

        for (MessageTypeAdapter messageAdapter : message.loaded()) {
            switch (messageAdapter.messageType()) {
                case TITLE -> {
                    if (messageAdapter instanceof TitleMessage(
                            String title1, String subtitle, int fadeIn, int stay, int fadeOut
                    )) {
                        senders.forEach(sender -> {
                            if (sender instanceof Player player) {
                                player.sendTitle(
                                        this.colorizeWithPlaceholders(title1, placeholders),
                                        this.colorizeWithPlaceholders(subtitle, placeholders),
                                        fadeIn, stay, fadeOut
                                );
                            }
                        });
                    }
                }
                case TCHAT -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        this.sendComponents(senders, simpleMessage, prefix, placeholders);
                    }
                }
                case ACTION_BAR -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        List<String> lines = this.getLines(simpleMessage, prefix, placeholders);
                        if (!lines.isEmpty()) {
                            String line = lines.getFirst();
                            senders.forEach(sender -> {
                                if (sender instanceof Player player) {
                                    player.sendActionBar(line);
                                }
                            });
                        }
                    }
                }
                case BOSS_BAR -> {
                    if (messageAdapter instanceof LegacyBossBarMessage(
                            String title, BarColor color, BarStyle style,
                            BarFlag[] flags, long duration, float progress
                    )) {
                        BossBar bossBar = flags != null
                                ? Bukkit.createBossBar(
                                this.colorizeWithPlaceholders(title, placeholders),
                                color,
                                style,
                                flags
                        )
                                : Bukkit.createBossBar(
                                this.colorizeWithPlaceholders(title, placeholders),
                                color,
                                style
                        );
                        bossBar.setProgress(Math.clamp(progress, 0f, 1f));
                        List<Player> snapshot = senders.stream()
                                .filter(s -> s instanceof Player)
                                .map(s -> (Player) s)
                                .toList();

                        snapshot.forEach(bossBar::addPlayer);

                        this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                                () -> {
                                    snapshot.forEach(bossBar::removePlayer);
                                    bossBar.setVisible(false);
                                },
                                duration / 50L);
                    }
                }
                case WITHOUT_PREFIX -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        this.sendComponents(senders, simpleMessage, false, placeholders);
                    }
                }
                case BROADCAST -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        Collection<CommandSender> online = Collections.unmodifiableCollection(
                                Bukkit.getOnlinePlayers()
                        );
                        this.sendComponents(online, simpleMessage, prefix, placeholders);
                    }
                }
            }
        }
    }
}