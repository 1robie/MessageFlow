package fr.robie.messageflow.formatter;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.robie.messageflow.api.ITextResolverRegistry;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.SoundMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Abstract base class for message formatters that handle text formatting and message delivery.
 * <p>
 * This class provides a caching mechanism for formatted messages and defines the contract
 * for sending various message types (chat, title, action bar) to players and command senders.
 * Subclasses implement the actual formatting logic (Adventure or Legacy).
 *
 * @param <T> the type of the plugin using this formatter
 * @param <V> the type of the formatted message object (e.g., Component for Adventure, String for Legacy)
 */
public abstract class MessageFormatter<T extends Plugin, V> {
    /**
     * The plugin instance using this formatter.
     */
    protected final T plugin;
    /**
     * Cache for storing formatted messages to avoid repeated parsing.
     */
    protected final LoadingCache<String, V> cache;

    /**
     * The text resolver registry used to resolve placeholders and hooks.
     */
    protected ITextResolverRegistry textResolverRegistry;

    /**
     * The getPrefix prepended to messages when enabled.
     */
    @NotNull
    protected String prefix = "";

    /**
     * Creates a new MessageFormatter with the specified plugin and configuration options.
     *
     * @param plugin the plugin instance
     */
    public MessageFormatter(@NotNull T plugin) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        this.plugin = plugin;
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .maximumSize(ConfigurationManager.Setting.MESSAGE_CACHE_MAX_SIZE.getValue());

        if (ConfigurationManager.Setting.MESSAGE_CACHE_INITIAL_CAPACITY.<Integer>getValue() > 0) {
            builder.initialCapacity(ConfigurationManager.Setting.MESSAGE_CACHE_INITIAL_CAPACITY.getValue());
        }

        if (ConfigurationManager.Setting.MESSAGE_CACHE_CONCURRENCY_LEVEL.<Integer>getValue() > 0) {
            builder.concurrencyLevel(ConfigurationManager.Setting.MESSAGE_CACHE_CONCURRENCY_LEVEL.getValue());
        }

        if (ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_ACCESS.<Long>getValue() > 0) {
            builder.expireAfterAccess(ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_ACCESS.getValue(), TimeUnit.MINUTES);
        }

        if (ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_WRITE.<Long>getValue() > 0) {
            builder.expireAfterWrite(ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_WRITE.getValue(), TimeUnit.MINUTES);
        }

        if (ConfigurationManager.Setting.MESSAGE_CACHE_RECORD_STATS.getValue()) {
            builder.recordStats();
        }

        if (ConfigurationManager.Setting.MESSAGE_CACHE_SOFT_VALUES.getValue()) {
            builder.softValues();
        }

        this.cache = builder.build(CacheLoader.from(this::load));
    }

    /**
     * Loads and formats a raw message string into the platform-specific format.
     *
     * @param message the raw message to format
     * @return the formatted message object
     */
    protected abstract V load(@NotNull String message);

    /**
     * Gets the plugin instance using this formatter.
     *
     * @return the plugin instance
     */
    public @NotNull T getPlugin() {
        return this.plugin;
    }

    /**
     * Gets the getPrefix prepended to messages when enabled.
     *
     * @return the getPrefix string
     */
    public @NotNull String getPrefix() {
        return this.prefix;
    }

    /**
     * Sets the getPrefix to prepend to messages when enabled.
     *
     * @param prefix the new getPrefix string
     */
    public void setPrefix(@NotNull String prefix) {
        Preconditions.checkNotNull(prefix, "Prefix cannot be null");
        this.prefix = prefix;
    }

    /**
     * Gets the text resolver registry used by this formatter.
     *
     * @return the text resolver registry
     */
    public ITextResolverRegistry getTextResolverRegistry() {
        return this.textResolverRegistry;
    }

    /**
     * Sets the text resolver registry to be used by this formatter.
     *
     * @param textResolverRegistry the new text resolver registry
     */
    public void setTextResolverRegistry(@NotNull ITextResolverRegistry textResolverRegistry) {
        Preconditions.checkNotNull(textResolverRegistry, "TextResolverRegistry cannot be null");
        this.textResolverRegistry = textResolverRegistry;
    }

    /**
     * Applies all registered text resolvers to the given text.
     *
     * @param text         the text to resolve
     * @param player       the player context, or null
     * @param placeholders optional placeholders for the resolvers
     * @return the resolved text
     */
    protected @NotNull String applyResolvers(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        if (this.textResolverRegistry == null) {
            return text;
        }
        return this.textResolverRegistry.resolve(text, player, placeholders);
    }

    /**
     * Sends a message to a collection of audiences, optionally using player context
     * if the registry has resolvers.
     *
     * @param <A>          the audience type
     * @param audiences    the collection of audiences
     * @param text         the raw text to format and send
     * @param placeholders the placeholders to apply
     * @param action       the action to perform for each audience
     */
    protected <A> void perAudienceOrShared(
            @NotNull Collection<? extends A> audiences,
            @NotNull String text,
            @NotNull Placeholder placeholders,
            @NotNull BiConsumer<A, V> action
    ) {
        if (this.textResolverRegistry.hasResolvers()) {
            audiences.forEach(audience -> {
                V formatted = this.format(text, audience instanceof Player p ? p : null, placeholders);
                action.accept(audience, formatted);
            });
        } else {
            V formatted = this.format(text, null, placeholders);
            audiences.forEach(audience -> action.accept(audience, formatted));
        }
    }

    /**
     * Sends multiple messages to a collection of audiences, optionally using player context.
     *
     * @param <A>          the audience type
     * @param audiences    the collection of audiences
     * @param texts        the raw texts to format and send
     * @param placeholders the placeholders to apply
     * @param prefix       optional prefix to prepend to each text
     * @param action       the action to perform for each audience/message
     */
    protected <A> void perAudienceOrShared(
            @NotNull Collection<? extends A> audiences,
            @NotNull Collection<String> texts,
            @NotNull Placeholder placeholders,
            @Nullable String prefix,
            @NotNull BiConsumer<A, V> action
    ) {
        if (this.textResolverRegistry.hasResolvers()) {
            audiences.forEach(audience -> {
                for (String text : texts) {
                    String withPrefix = prefix != null ? prefix + text : text;
                    V formatted = this.format(withPrefix, audience instanceof Player p ? p : null, placeholders);
                    action.accept(audience, formatted);
                }
            });
        } else {
            List<V> formattedList = texts.stream()
                    .map(text -> {
                        String withPrefix = prefix != null ? prefix + text : text;
                        return this.format(withPrefix, null, placeholders);
                    })
                    .toList();
            audiences.forEach(audience -> formattedList.forEach(formatted -> action.accept(audience, formatted)));
        }
    }

    /**
     * Formats a raw message string by applying placeholders, text resolvers, and loading it
     * into the platform-specific format.
     *
     * @param message      the raw message string
     * @param player       the player context (optional)
     * @param placeholders the placeholders to apply
     * @return the formatted message object
     */
    @NotNull
    protected V format(@Nullable String message, @Nullable Player player, @NotNull Placeholder placeholders) {
        if (message == null) {
            return this.empty();
        }
        if (placeholders.isEmpty() && !this.textResolverRegistry.hasResolvers()) {
            return this.cache.getUnchecked(message);
        }
        String parsedText = placeholders.parse(message);
        parsedText = this.applyResolvers(parsedText, player, placeholders);
        return this.load(parsedText);
    }

    /**
     * Provides an empty value of the formatted message type, used when the input message is null.
     *
     * @return an empty formatted message object
     */
    @NotNull
    protected abstract V empty();

    /**
     * Schedules the removal of a boss bar after a certain duration.
     *
     * @param <A>           the player/audience type
     * @param <B>           the boss bar type
     * @param durationTicks the duration in ticks
     * @param players       the players to hide the boss bar from
     * @param hideAction    the action to hide the boss bar
     * @param bossbar       the boss bar instance
     */
    protected <A, B> void scheduleHideBossBar(
            long durationTicks,
            Collection<? extends A> players,
            BiConsumer<A, B> hideAction,
            B bossbar
    ) {
        this.plugin.getServer().getAsyncScheduler().runDelayed(this.plugin,
                w -> players.forEach(player -> hideAction.accept(player, bossbar)),
                durationTicks * 50L,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Schedules the removal of a boss bar for a single player after a certain duration.
     *
     * @param <A>           the player/audience type
     * @param <B>           the boss bar type
     * @param durationTicks the duration in ticks
     * @param player        the player to hide the boss bar from
     * @param hideAction    the action to hide the boss bar
     * @param bossbar       the boss bar instance
     */
    protected <A, B> void scheduleHideBossBar(
            long durationTicks,
            A player,
            BiConsumer<A, B> hideAction,
            B bossbar
    ) {
        this.scheduleHideBossBar(durationTicks, Collections.singleton(player), hideAction, bossbar);
    }

    /**
     * Sends a title to a single player.
     *
     * @param player       the player to send the title to
     * @param title        the main title text
     * @param subtitle     the subtitle text
     * @param fadeIn       fade-in time in ticks
     * @param stay         stay time in ticks
     * @param fadeOut      fade-out time in ticks
     * @param placeholders placeholders for text replacement
     */
    public void sendTitle(@Nullable Player player, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Placeholder placeholders) {
        if (player == null) {
            return;
        }
        this.sendTitle(Collections.singleton(player), title, subtitle, fadeIn, stay, fadeOut, placeholders);
    }

    /**
     * Sends a title to a single player without placeholders.
     *
     * @param player   the player to send the title to
     * @param title    the main title text
     * @param subtitle the subtitle text
     * @param fadeIn   fade-in time in ticks
     * @param stay     stay time in ticks
     * @param fadeOut  fade-out time in ticks
     */
    public void sendTitle(@Nullable Player player, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
        this.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut, Placeholder.empty());
    }

    /**
     * Sends a title to multiple players.
     *
     * @param players      the players to send the title to
     * @param title        the main title text
     * @param subtitle     the subtitle text
     * @param fadeIn       fade-in time in ticks
     * @param stay         stay time in ticks
     * @param fadeOut      fade-out time in ticks
     * @param placeholders placeholders for text replacement
     */
    public abstract void sendTitle(@NotNull Collection<? extends @NotNull Player> players, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Placeholder placeholders);

    /**
     * Sends an action bar message to a single player without getPrefix.
     *
     * @param player       the player to send the action bar to
     * @param message      the action bar text
     * @param placeholders placeholders for text replacement
     */
    public void sendActionBar(@Nullable Player player, @Nullable String message, @NotNull Placeholder placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, placeholders);
    }

    /**
     * Sends an action bar message to a single player without getPrefix or placeholders.
     *
     * @param player  the player to send the action bar to
     * @param message the action bar text
     */
    public void sendActionBar(@Nullable Player player, @Nullable String message) {
        this.sendActionBar(player, message, Placeholder.empty());
    }

    /**
     * Sends an action bar message to multiple players without getPrefix.
     *
     * @param players      the players to send the action bar to
     * @param message      the action bar text
     * @param placeholders placeholders for text replacement
     */
    public abstract void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, @NotNull Placeholder placeholders);

    /**
     * Sends an action bar message to a single player with optional getPrefix.
     *
     * @param player       the player to send the action bar to
     * @param message      the action bar text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public void sendActionBar(@Nullable Player player, @Nullable String message, boolean prefix, @NotNull Placeholder placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, prefix, placeholders);
    }

    /**
     * Sends an action bar message to a single player with optional getPrefix, no placeholders.
     *
     * @param player  the player to send the action bar to
     * @param message the action bar text
     * @param prefix  whether to prepend the configured getPrefix
     */
    public void sendActionBar(@Nullable Player player, @Nullable String message, boolean prefix) {
        this.sendActionBar(player, message, prefix, Placeholder.empty());
    }

    /**
     * Sends an action bar message to multiple players with optional getPrefix.
     *
     * @param players      the players to send the action bar to
     * @param message      the action bar text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public abstract void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, boolean prefix, @NotNull Placeholder placeholders);

    /**
     * Broadcasts an action bar message to all online players with getPrefix enabled.
     *
     * @param message      the action bar text
     * @param placeholders placeholders for text replacement
     */
    public void broadcastActionBar(@Nullable String message, @NotNull Placeholder placeholders) {
        this.broadcastActionBar(message, true, placeholders);
    }

    /**
     * Broadcasts an action bar message to all online players with getPrefix enabled, no placeholders.
     *
     * @param message the action bar text
     */
    public void broadcastActionBar(@Nullable String message) {
        this.broadcastActionBar(message, true, Placeholder.empty());
    }

    /**
     * Broadcasts an action bar message to all online players with optional getPrefix.
     *
     * @param message      the action bar text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public abstract void broadcastActionBar(@Nullable String message, boolean prefix, @NotNull Placeholder placeholders);

    /**
     * Broadcasts a title to all online players.
     *
     * @param title        the main title text
     * @param subtitle     the subtitle text
     * @param fadeIn       fade-in time in ticks
     * @param stay         stay time in ticks
     * @param fadeOut      fade-out time in ticks
     * @param placeholders placeholders for text replacement
     */
    public void broadcastTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Placeholder placeholders) {
        this.sendTitle(Bukkit.getOnlinePlayers(), title, subtitle, fadeIn, stay, fadeOut, placeholders);
    }

    /**
     * Broadcasts a title to all online players, no placeholders.
     *
     * @param title    the main title text
     * @param subtitle the subtitle text
     * @param fadeIn   fade-in time in ticks
     * @param stay     stay time in ticks
     * @param fadeOut  fade-out time in ticks
     */
    public void broadcastTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
        this.broadcastTitle(title, subtitle, fadeIn, stay, fadeOut, Placeholder.empty());
    }

    /**
     * Sends a chat message to a single command sender with getPrefix enabled.
     *
     * @param sender       the command sender to send the message to
     * @param message      the chat message text
     * @param placeholders placeholders for text replacement
     */
    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, @NotNull Placeholder placeholders) {
        this.sendMessage(sender, message, true, placeholders);
    }

    /**
     * Sends a chat message to a single command sender with getPrefix enabled, no placeholders.
     *
     * @param sender  the command sender to send the message to
     * @param message the chat message text
     */
    public void sendMessage(@Nullable CommandSender sender, @Nullable String message) {
        this.sendMessage(sender, message, true, Placeholder.empty());
    }

    /**
     * Sends a chat message to a single command sender with optional getPrefix.
     *
     * @param sender       the command sender to send the message to
     * @param message      the chat message text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, boolean prefix, @NotNull Placeholder placeholders) {
        if (sender == null || message == null) {
            return;
        }
        this.sendMessage(Collections.singleton(sender), message, prefix, placeholders);
    }

    /**
     * Sends a chat message to a single command sender with optional getPrefix, no placeholders.
     *
     * @param sender  the command sender to send the message to
     * @param message the chat message text
     * @param prefix  whether to prepend the configured getPrefix
     */
    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, boolean prefix) {
        this.sendMessage(sender, message, prefix, Placeholder.empty());
    }

    /**
     * Sends a chat message to multiple command senders with getPrefix enabled.
     *
     * @param senders      the command senders to send the message to
     * @param message      the chat message text
     * @param placeholders placeholders for text replacement
     */
    public void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, @NotNull Placeholder placeholders) {
        Preconditions.checkNotNull(senders, "Senders collection cannot be null");
        this.sendMessage(senders, message, true, placeholders);
    }

    /**
     * Sends a chat message to multiple command senders with getPrefix enabled, no placeholders.
     *
     * @param senders the command senders to send the message to
     * @param message the chat message text
     */
    public void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message) {
        this.sendMessage(senders, message, true, Placeholder.empty());
    }

    /**
     * Sends a chat message to multiple command senders with optional getPrefix.
     *
     * @param senders      the command senders to send the message to
     * @param message      the chat message text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public abstract void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, boolean prefix, @NotNull Placeholder placeholders);

    /**
     * Broadcasts a chat message to all online players with getPrefix enabled.
     *
     * @param message      the chat message text
     * @param placeholders placeholders for text replacement
     */
    public void broadcast(@Nullable String message, @NotNull Placeholder placeholders) {
        this.broadcast(message, true, placeholders);
    }

    /**
     * Broadcasts a chat message to all online players with getPrefix enabled, no placeholders.
     *
     * @param message the chat message text
     */
    public void broadcast(@Nullable String message) {
        this.broadcast(message, true, Placeholder.empty());
    }

    /**
     * Broadcasts a chat message to all online players with optional getPrefix.
     *
     * @param message      the chat message text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public abstract void broadcast(@Nullable String message, boolean prefix, @NotNull Placeholder placeholders);

    /**
     * Plays a sound for a single player.
     *
     * @param player       the player to play the sound for
     * @param soundMessage the sound message to play
     */
    public void playSound(@NotNull Player player, @NotNull SoundMessage soundMessage) {
        this.playSound(Collections.singleton(player), soundMessage);
    }

    /**
     * Plays a sound for multiple players.
     *
     * @param players      the players to play the sound for
     * @param soundMessage the sound message to play
     */
    public void playSound(@NotNull Collection<? extends Player> players, @NotNull SoundMessage soundMessage) {
        if (players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            player.playSound(player, soundMessage.sound(), soundMessage.category(), soundMessage.volume(), soundMessage.pitch());
        }
    }

    /**
     * Broadcasts a sound to all online players.
     *
     * @param soundMessage the sound message to play
     */
    public void broadcastSound(@NotNull SoundMessage soundMessage) {
        this.playSound(Bukkit.getOnlinePlayers(), soundMessage);
    }

    /**
     * Sends a chat message to a single command sender without getPrefix.
     *
     * @param sender       the command sender to send the message to
     * @param message      the chat message text
     * @param placeholders placeholders for text replacement
     */
    public void sendMessageWithoutPrefix(@Nullable CommandSender sender, @Nullable String message, @NotNull Placeholder placeholders) {
        this.sendMessage(sender, message, false, placeholders);
    }

    /**
     * Sends a chat message to a single command sender without getPrefix or placeholders.
     *
     * @param sender  the command sender to send the message to
     * @param message the chat message text
     */
    public void sendMessageWithoutPrefix(@Nullable CommandSender sender, @Nullable String message) {
        this.sendMessage(sender, message, false, Placeholder.empty());
    }

    /**
     * Sends a resolved message to a single command sender with getPrefix enabled.
     *
     * @param message      the message definition to resolve and send
     * @param sender       the command sender to send the message to
     * @param placeholders placeholders for text replacement
     */
    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, @NotNull Placeholder placeholders) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(sender, "Sender cannot be null");
        this.sendMessage(message, sender, true, placeholders);
    }

    /**
     * Sends a resolved message to a single command sender with getPrefix enabled, no placeholders.
     *
     * @param message the message definition to resolve and send
     * @param sender  the command sender to send the message to
     */
    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender) {
        this.sendMessage(message, sender, true, Placeholder.empty());
    }

    /**
     * Sends a resolved message to multiple command senders with getPrefix enabled.
     *
     * @param message      the message definition to resolve and send
     * @param senders      the command senders to send the message to
     * @param placeholders placeholders for text replacement
     */
    public void sendMessage(@NotNull Message message, @NotNull Collection<? extends CommandSender> senders, @NotNull Placeholder placeholders) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(senders, "Senders collection cannot be null");
        this.sendMessage(message, senders, true, placeholders);
    }

    /**
     * Sends a resolved message to multiple command senders with getPrefix enabled, no placeholders.
     *
     * @param message the message definition to resolve and send
     * @param senders the command senders to send the message to
     */
    public void sendMessage(@NotNull Message message, @NotNull Collection<? extends CommandSender> senders) {
        this.sendMessage(message, senders, true, Placeholder.empty());
    }

    /**
     * Sends a resolved message to a single command sender with optional getPrefix.
     *
     * @param message      the message definition to resolve and send
     * @param sender       the command sender to send the message to
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, boolean prefix, @NotNull Placeholder placeholders) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(sender, "Sender cannot be null");
        this.sendMessage(message, Collections.singleton(sender), prefix, placeholders);
    }

    /**
     * Sends a resolved message to a single command sender with optional getPrefix, no placeholders.
     *
     * @param message the message definition to resolve and send
     * @param sender  the command sender to send the message to
     * @param prefix  whether to prepend the configured getPrefix
     */
    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, boolean prefix) {
        this.sendMessage(message, sender, prefix, Placeholder.empty());
    }

    /**
     * Sends a resolved message to multiple command senders with optional getPrefix.
     *
     * @param message      the message definition to resolve and send
     * @param senders      the command senders to send the message to
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders placeholders for text replacement
     */
    public abstract void sendMessage(@NotNull Message message, @NotNull Collection<? extends CommandSender> senders, boolean prefix, @NotNull Placeholder placeholders);

    /**
     * Sends a resolved message to the console with a getPrefix.
     *
     * @param message      the message definition to resolve and send
     * @param logType      the type of log message
     * @param sender       the console sender to send the message to
     * @param placeholders placeholders for text replacement
     */
    public abstract void sendMessage(@NotNull Message message, @NotNull Logger.LogType logType, @NotNull ConsoleCommandSender sender, @NotNull Placeholder placeholders);

    /**
     * Sends a resolved message to the console with a getPrefix, no placeholders.
     *
     * @param message the message definition to resolve and send
     * @param logType the type of log message
     * @param sender  the console sender to send the message to
     */
    public void sendMessage(@NotNull Message message, @NotNull Logger.LogType logType, @NotNull ConsoleCommandSender sender) {
        this.sendMessage(message, logType, sender, Placeholder.empty());
    }

    public void clearCache() {
        this.cache.cleanUp();

    }
}
