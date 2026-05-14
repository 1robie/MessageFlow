package fr.robie.messageflow.logger;

import fr.robie.messageflow.formatter.LegacyMessageFormatter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Logger implementation that uses Legacy (ChatColor) for formatting.
 * <p>
 * This logger is suitable for older Minecraft versions or environments
 * where Adventure/MiniMessage is not available or preferred.
 */
public final class LegacyLogger extends Logger {

    /**
     * Constructs a new LegacyLogger.
     *
     * @param prefix           the log prefix
     * @param messageFormatter the Legacy message formatter to use
     */
    public LegacyLogger(@NotNull String prefix, @NotNull LegacyMessageFormatter<? extends Plugin> messageFormatter) {
        super(prefix, messageFormatter);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull String message, @NotNull Object... args) {
        String colorForLogType = this.getColorForLogType(type);

        String fullMessage = colorForLogType + "[" + type.name() + "] &r" + this.prefix + " " + (this.colorWholeMessage ? colorForLogType : "") + message;
        this.messageFormatter.sendMessage(Bukkit.getConsoleSender(), fullMessage, false, args);
    }

    @Override
    protected @NotNull String getColorForLogType(@NotNull LogType type) {
        return switch (type) {
            case INFO -> "&9";
            case WARNING -> "&6";
            case ERROR -> "&c";
            case DEBUG -> "&d";
        };
    }
}
