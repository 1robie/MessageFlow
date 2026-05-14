package fr.robie.messageflow.logger;

import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.model.Message;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Logger implementation that uses Adventure (MiniMessage) for formatting.
 * <p>
 * This logger is suitable for modern Minecraft versions (1.16+) that support
 * rich text components and MiniMessage formatting.
 */
public final class ComponentLogger extends Logger {

    /**
     * Constructs a new ComponentLogger.
     *
     * @param prefix           the log getPrefix
     * @param messageFormatter the Adventure message formatter to use
     */
    public ComponentLogger(@NotNull String prefix, @NotNull AdventureMessageFormatter<? extends Plugin> messageFormatter) {
        super(prefix, messageFormatter);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull String message, @NotNull Object... args) {
        String fullMessage = this.prefixe(type) + message;
        this.messageFormatter.sendMessage(Bukkit.getConsoleSender(), fullMessage, false, args);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull Message message, @NonNull @NotNull Object... args) {
        this.messageFormatter.sendMessage(message, type, Bukkit.getConsoleSender(), args);
    }

    @Override
    @NotNull
    protected String prefixe(@NotNull LogType type) {
        String colorForLogType = this.getColorForLogType(type);
        return colorForLogType + "[" + type.name() + "] <reset>" + this.prefix + " " + (this.colorWholeMessage ? colorForLogType : "");
    }

    @Override
    protected @NotNull String getColorForLogType(@NotNull LogType type) {
        return switch (type) {
            case INFO -> "<blue>";
            case WARNING -> "<gold>";
            case ERROR -> "<red>";
            case DEBUG -> "<light_purple>";
        };
    }
}
