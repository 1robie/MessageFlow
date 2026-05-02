package fr.robie.messageflow.format;

import fr.robie.messageflow.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class MessageFormatter<T extends Plugin> implements TextFormatter {
    protected final T plugin;

    @NotNull
    protected String prefix = "";

    public MessageFormatter(@NotNull T plugin) {
        assert plugin != null : "Plugin cannot be null";
        this.plugin = plugin;
    }

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

    public abstract void sendTitle(@NotNull Collection<@NotNull Player> players, @Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut, @NotNull Object... placeholders);

    public void sendActionBar(@Nullable Player player, @Nullable String message, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, placeholders);
    }

    public abstract void sendActionBar(@NotNull Collection<@NotNull Player> players, @Nullable String message, @NotNull Object... placeholders);

    public void sendActionBar(@Nullable Player player, @Nullable String message, boolean prefix, @NotNull Object... placeholders) {
        if (player == null) {
            return;
        }
        this.sendActionBar(Collections.singleton(player), message, prefix, placeholders);
    }

    public abstract void sendActionBar(@NotNull Collection<@NotNull Player> players, @Nullable String message, boolean prefix, @NotNull Object... placeholders);

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

