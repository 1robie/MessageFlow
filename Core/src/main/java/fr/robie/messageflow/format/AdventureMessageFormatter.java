package fr.robie.messageflow.format;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.robie.messageflow.Message;
import fr.robie.messageflow.MessageTypeAdapter;
import fr.robie.messageflow.message.PaperBossBarMessage;
import fr.robie.messageflow.message.SimpleMessage;
import fr.robie.messageflow.message.TitleMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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

public class AdventureMessageFormatter<T extends Plugin> extends MessageFormatter<T> {

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

    private static final LoadingCache<String, Component> CACHE = CacheBuilder.newBuilder()
            .maximumSize(512)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(msg -> MINI_MESSAGE.deserialize(colorMiniMessage(msg))));


    public AdventureMessageFormatter(@NotNull T plugin) {
        super(plugin);
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


    @NotNull
    public Component getComponent(@NotNull String message) {
        return CACHE.getUnchecked(message);
    }

    private Component getComponentWithPlaceholders(@Nullable String message, @NotNull Object... placeholders) {
        if (message == null) {
            return Component.empty();
        }
        if (placeholders.length == 0) {
            return this.getComponent(message);
        }
        return MINI_MESSAGE.deserialize(colorMiniMessage(this.parseText(message, placeholders)));
    }


    private void sendComponents(
            @NotNull Collection<? extends Audience> audiences,
            @NotNull SimpleMessage message,
            boolean prefix,
            @NotNull Object[] placeholders,
            @NotNull BiConsumer<Audience, Component> sender
    ) {
        List<Component> components = this.getComponents(message, prefix, placeholders);
        if (!components.isEmpty()) {
            this.sendToAudiences(audiences, components, sender);
        }
    }

    private List<Component> getComponents(@NotNull SimpleMessage message, boolean prefix, @NotNull Object[] placeholders) {
        List<String> messages = message.messages();
        if (messages.isEmpty() || messages.stream().allMatch(s -> s == null || s.isBlank())) {
            return Collections.emptyList();
        }

        String prefixText = prefix ? this.prefix : "";

        return messages.stream()
                .map(s -> s == null ? Component.empty() : this.getComponentWithPlaceholders(prefixText + s, placeholders))
                .toList();
    }

    private void sendToAudiences(
            @NotNull Collection<? extends Audience> audiences,
            @NotNull List<Component> components,
            @NotNull BiConsumer<Audience, Component> sender
    ) {
        audiences.forEach(audience -> components.forEach(component -> sender.accept(audience, component)));
    }

    @Override
    public void sendTitle(@NotNull Collection<@NotNull Player> players, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NonNull @NotNull Object... placeholders) {
        Title titleObj = Title.title(
                this.getComponentWithPlaceholders(title, placeholders),
                this.getComponentWithPlaceholders(subtitle, placeholders),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );
        players.forEach(player -> player.showTitle(titleObj));
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
        Component component = this.getComponentWithPlaceholders(prefixText + message, placeholders);
        players.forEach(player -> player.sendActionBar(component));
    }

    @Override
    public void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (message == null || senders.isEmpty()) {
            return;
        }

        String prefixText = prefix ? this.prefix : "";
        Component component = this.getComponentWithPlaceholders(prefixText + message, placeholders);

        senders.forEach(sender -> {
            if (sender instanceof Audience audience) {
                audience.sendMessage(component);
            }
        });
    }

    @Override
    public void broadcast(@Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (message == null) {
            return;
        }

        String prefixText = prefix ? this.prefix : "";
        Component component = this.getComponentWithPlaceholders(prefixText + message, placeholders);
        Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(component);
    }

    public void sendMessage(@NotNull Message message, @NotNull Collection<CommandSender> audiences, boolean prefix, @NotNull Object... placeholders) {
        if (audiences.isEmpty()) {
            return;
        }

        for (MessageTypeAdapter messageAdapter : message.loaded()) {
            switch (messageAdapter.messageType()) {
                case TITLE -> {
                    if (messageAdapter instanceof TitleMessage(
                            String title1, String subtitle, int fadeIn, int stay, int fadeOut
                    )) {
                        Title title = Title.title(
                                this.getComponentWithPlaceholders(title1, placeholders),
                                this.getComponentWithPlaceholders(subtitle, placeholders),
                                Title.Times.times(
                                        Duration.ofMillis(fadeIn * 50L),
                                        Duration.ofMillis(stay * 50L),
                                        Duration.ofMillis(fadeOut * 50L)
                                )
                        );
                        audiences.forEach(a -> a.showTitle(title));
                    }
                }
                case TCHAT -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        this.sendComponents(audiences, simpleMessage, prefix, placeholders, Audience::sendMessage);
                    }
                }
                case ACTION_BAR -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        this.sendComponents(audiences, simpleMessage, prefix, placeholders, Audience::sendActionBar);
                    }
                }
                case BOSS_BAR -> {
                    if (messageAdapter instanceof PaperBossBarMessage(
                            String title, BossBar.Color color, BossBar.Overlay overlay,
                            Set<BossBar.Flag> flags, long duration, float progress
                    )) {
                        BossBar bossBar = BossBar.bossBar(
                                this.getComponentWithPlaceholders(title, placeholders),
                                progress, color, overlay, flags
                        );
                        List<Audience> snapshot = List.copyOf(audiences);
                        snapshot.forEach(a -> a.showBossBar(bossBar));
                        this.plugin.getServer().getAsyncScheduler().runDelayed(
                                this.plugin,
                                w -> snapshot.forEach(a -> a.hideBossBar(bossBar)),
                                duration * 50L,
                                TimeUnit.MILLISECONDS
                        );
                    }
                }
                case WITHOUT_PREFIX -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        this.sendComponents(audiences, simpleMessage, false, placeholders, Audience::sendMessage);
                    }
                }
                case BROADCAST -> {
                    if (messageAdapter instanceof SimpleMessage simpleMessage) {
                        ForwardingAudience broadcast = Audience.audience(Bukkit.getOnlinePlayers());
                        this.sendComponents(Collections.singleton(broadcast), simpleMessage, prefix, placeholders, Audience::sendMessage);
                    }
                }
            }
        }
    }
}