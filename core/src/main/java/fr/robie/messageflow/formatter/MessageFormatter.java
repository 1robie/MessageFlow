package fr.robie.messageflow.formatter;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
public abstract class MessageFormatter<T extends Plugin, V> implements TextFormatter {
    /**
     * The plugin instance using this formatter.
     */
    protected final T plugin;
    /**
     * Cache for storing formatted messages to avoid repeated parsing.
     */
    protected final LoadingCache<String, V> cache;

    /**
     * The getPrefix prepended to messages when enabled.
     */
    @NotNull
    protected String prefix = "";

    /**
     * Creates a new MessageFormatter with the specified plugin and configuration options.
     *
     * @param plugin  the plugin instance
     * @param options the configuration options for the message cache
     */
    public MessageFormatter(@NotNull T plugin, @NotNull ConfigurationOptions<?> options) {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null");
        Preconditions.checkNotNull(options, "Configuration options cannot be null");
        this.plugin = plugin;
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .maximumSize(options.cacheMaximumSize());

        if (options.cacheInitialCapacity() > 0) {
            builder.initialCapacity(options.cacheInitialCapacity());
        }

        if (options.cacheConcurrencyLevel() > 0) {
            builder.concurrencyLevel(options.cacheConcurrencyLevel());
        }

        if (options.cacheExpireAfterAccessMinutes() > 0) {
            builder.expireAfterAccess(options.cacheExpireAfterAccessMinutes(), TimeUnit.MINUTES);
        }

        if (options.cacheExpireAfterWriteMinutes() > 0) {
            builder.expireAfterWrite(options.cacheExpireAfterWriteMinutes(), TimeUnit.MINUTES);
        }

        if (options.cacheRecordStats()) {
            builder.recordStats();
        }

        if (options.cacheSoftValues()) {
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
     * Sends a title to a single player.
     *
     * @param player       the player to send the title to
     * @param title        the main title text
     * @param subtitle     the subtitle text
     * @param fadeIn       fade-in time in ticks
     * @param stay         stay time in ticks
     * @param fadeOut      fade-out time in ticks
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendTitle(@Nullable Player player, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendTitle(Collections.singleton(player), title, subtitle, fadeIn, stay, fadeOut, placeholders);
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
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void sendTitle(@NotNull Collection<? extends @NotNull Player> players, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders);

    /**
     * Sends an action bar message to a single player without getPrefix.
     *
     * @param player       the player to send the action bar to
     * @param message      the action bar text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendActionBar(@Nullable Player player, @Nullable String message, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, placeholders);
    }

    /**
     * Sends an action bar message to multiple players without getPrefix.
     *
     * @param players      the players to send the action bar to
     * @param message      the action bar text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, @NotNull Object... placeholders);

    /**
     * Sends an action bar message to a single player with optional getPrefix.
     *
     * @param player       the player to send the action bar to
     * @param message      the action bar text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendActionBar(@Nullable Player player, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, prefix, placeholders);
    }

    /**
     * Sends an action bar message to multiple players with optional getPrefix.
     *
     * @param players      the players to send the action bar to
     * @param message      the action bar text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, boolean prefix, @NotNull Object... placeholders);

    /**
     * Broadcasts an action bar message to all online players with getPrefix enabled.
     *
     * @param message      the action bar text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void broadcastActionBar(@Nullable String message, @NotNull Object... placeholders) {
        this.broadcastActionBar(message, true, placeholders);
    }

    /**
     * Broadcasts an action bar message to all online players with optional getPrefix.
     *
     * @param message      the action bar text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void broadcastActionBar(@Nullable String message, boolean prefix, @NotNull Object... placeholders);

    /**
     * Broadcasts a title to all online players.
     *
     * @param title        the main title text
     * @param subtitle     the subtitle text
     * @param fadeIn       fade-in time in ticks
     * @param stay         stay time in ticks
     * @param fadeOut      fade-out time in ticks
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void broadcastTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders) {
        this.sendTitle(Bukkit.getOnlinePlayers(), title, subtitle, fadeIn, stay, fadeOut, placeholders);
    }

    /**
     * Sends a chat message to a single command sender with getPrefix enabled.
     *
     * @param sender       the command sender to send the message to
     * @param message      the chat message text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, @NotNull Object... placeholders) {
        this.sendMessage(sender, message, true, placeholders);
    }

    /**
     * Sends a chat message to a single command sender with optional getPrefix.
     *
     * @param sender       the command sender to send the message to
     * @param message      the chat message text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (sender == null || message == null) {
            return;
        }
        this.sendMessage(Collections.singleton(sender), message, prefix, placeholders);
    }

    /**
     * Sends a chat message to multiple command senders with getPrefix enabled.
     *
     * @param senders      the command senders to send the message to
     * @param message      the chat message text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, @NotNull Object... placeholders) {
        Preconditions.checkNotNull(senders, "Senders collection cannot be null");
        this.sendMessage(senders, message, true, placeholders);
    }

    /**
     * Sends a chat message to multiple command senders with optional getPrefix.
     *
     * @param senders      the command senders to send the message to
     * @param message      the chat message text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, boolean prefix, @NotNull Object... placeholders);

    /**
     * Broadcasts a chat message to all online players with getPrefix enabled.
     *
     * @param message      the chat message text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void broadcast(@Nullable String message, @NotNull Object... placeholders) {
        this.broadcast(message, true, placeholders);
    }

    /**
     * Broadcasts a chat message to all online players with optional getPrefix.
     *
     * @param message      the chat message text
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void broadcast(@Nullable String message, boolean prefix, @NotNull Object... placeholders);

    /**
     * Sends a chat message to a single command sender without getPrefix.
     *
     * @param sender       the command sender to send the message to
     * @param message      the chat message text
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessageWithoutPrefix(@Nullable CommandSender sender, @Nullable String message, @NotNull Object... placeholders) {
        this.sendMessage(sender, message, false, placeholders);
    }

    /**
     * Sends a resolved message to a single command sender with getPrefix enabled.
     *
     * @param message      the message definition to resolve and send
     * @param sender       the command sender to send the message to
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, @NotNull Object... placeholders) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(sender, "Sender cannot be null");
        this.sendMessage(message, sender, true, placeholders);
    }

    /**
     * Sends a resolved message to multiple command senders with getPrefix enabled.
     *
     * @param message      the message definition to resolve and send
     * @param senders      the command senders to send the message to
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessage(@NotNull Message message, @NotNull Collection<? extends CommandSender> senders, @NotNull Object... placeholders) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(senders, "Senders collection cannot be null");
        this.sendMessage(message, senders, true, placeholders);
    }

    /**
     * Sends a resolved message to a single command sender with optional getPrefix.
     *
     * @param message      the message definition to resolve and send
     * @param sender       the command sender to send the message to
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, boolean prefix, @NotNull Object... placeholders) {
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(sender, "Sender cannot be null");
        this.sendMessage(message, Collections.singleton(sender), prefix, placeholders);
    }

    /**
     * Sends a resolved message to multiple command senders with optional getPrefix.
     *
     * @param message      the message definition to resolve and send
     * @param senders      the command senders to send the message to
     * @param prefix       whether to prepend the configured getPrefix
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void sendMessage(@NotNull Message message, @NotNull Collection<? extends CommandSender> senders, boolean prefix, @NotNull Object... placeholders);

    /**
     * Sends a resolved message to the console with a getPrefix.
     *
     * @param message      the message definition to resolve and send
     * @param sender       the console sender to send the message to
     * @param placeholders key-value pairs for placeholder replacement
     */
    public abstract void sendMessage(@NotNull Message message, @NotNull Logger.LogType logType, @NotNull ConsoleCommandSender sender, @NotNull Object... placeholders);
}
