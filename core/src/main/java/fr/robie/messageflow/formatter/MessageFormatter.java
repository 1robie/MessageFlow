package fr.robie.messageflow.formatter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.robie.messageflow.configuration.ConfigurationOptions;
import fr.robie.messageflow.model.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public abstract class MessageFormatter<T extends Plugin, V> implements TextFormatter {
    protected final T plugin;
    protected final LoadingCache<String, V> cache;

    @NotNull
    protected String prefix = "";

    public MessageFormatter(@NotNull T plugin, @NotNull ConfigurationOptions options) {
        assert plugin != null : "Plugin cannot be null";
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

    protected abstract V load(@NotNull String message);

    public @NotNull T getPlugin() {
        return this.plugin;
    }

    public @NotNull String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(@NotNull String prefix) {
        assert prefix != null : "Prefix cannot be null";
        this.prefix = prefix;
    }

    public void sendTitle(@Nullable Player player, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendTitle(Collections.singleton(player), title, subtitle, fadeIn, stay, fadeOut, placeholders);
    }

    public abstract void sendTitle(@NotNull Collection<? extends @NotNull Player> players, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders);

    public void sendActionBar(@Nullable Player player, @Nullable String message, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, placeholders);
    }

    public abstract void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, @NotNull Object... placeholders);

    public void sendActionBar(@Nullable Player player, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, prefix, placeholders);
    }

    public abstract void sendActionBar(@NotNull Collection<? extends @NotNull Player> players, @Nullable String message, boolean prefix, @NotNull Object... placeholders);

    public void broadcastActionBar(@Nullable String message, @NotNull Object... placeholders) {
        this.broadcastActionBar(message, true, placeholders);
    }

    public abstract void broadcastActionBar(@Nullable String message, boolean prefix, @NotNull Object... placeholders);

    public void broadcastTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders) {
        this.sendTitle(Bukkit.getOnlinePlayers(), title, subtitle, fadeIn, stay, fadeOut, placeholders);
    }

    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, @NotNull Object... placeholders) {
        this.sendMessage(sender, message, true, placeholders);
    }

    public void sendMessage(@Nullable CommandSender sender, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (sender == null || message == null) {
            return;
        }
        this.sendMessage(Collections.singleton(sender), message, prefix, placeholders);
    }

    public void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, @NotNull Object... placeholders) {
        this.sendMessage(senders, message, true, placeholders);
    }

    public abstract void sendMessage(@NotNull Collection<? extends CommandSender> senders, @Nullable String message, boolean prefix, @NotNull Object... placeholders);

    public void broadcast(@Nullable String message, @NotNull Object... placeholders) {
        this.broadcast(message, true, placeholders);
    }

    public abstract void broadcast(@Nullable String message, boolean prefix, @NotNull Object... placeholders);

    public void sendMessageWithoutPrefix(@Nullable CommandSender sender, @Nullable String message, @NotNull Object... placeholders) {
        this.sendMessage(sender, message, false, placeholders);
    }

    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, @NotNull Object... placeholders) {
        this.sendMessage(message, sender, true, placeholders);
    }

    public void sendMessage(@NotNull Message message, @NotNull Collection<CommandSender> senders, @NotNull Object... placeholders) {
        this.sendMessage(message, senders, true, placeholders);
    }

    public void sendMessage(@NotNull Message message, @NotNull CommandSender sender, boolean prefix, @NotNull Object... placeholders) {
        this.sendMessage(message, Collections.singleton(sender), prefix, placeholders);
    }

    public abstract void sendMessage(@NotNull Message message, @NotNull Collection<CommandSender> senders, boolean prefix, @NotNull Object... placeholders);
}

