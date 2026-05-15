package fr.robie.messageflow.logger;

import com.google.common.base.Throwables;
import fr.robie.messageflow.formatter.LegacyMessageFormatter;
import fr.robie.messageflow.model.Message;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

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
     * @param prefix           the log getPrefix
     * @param messageFormatter the Legacy message formatter to use
     */
    public LegacyLogger(@NotNull String prefix, @NotNull LegacyMessageFormatter<? extends Plugin> messageFormatter) {
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
    protected void log(@NotNull LogType type, @NotNull Throwable throwable, @NotNull String message, @NotNull Object... args) {
        String fullMessage = this.prefixe(type) + message + "\n&c" + Throwables.getStackTraceAsString(throwable);
        this.messageFormatter.sendMessage(Bukkit.getConsoleSender(), fullMessage, false, args);
    }

    @Override
    @NotNull
    protected String prefixe(@NotNull LogType type) {
        String colorForLogType = type.getLegacyColorCode();
        String typeName = (this.showTypeNames && type.isShowTypeName()) ? colorForLogType + "[" + type.name() + "] " : "";
        return typeName + "&r" + this.prefix + " " + (this.colorWholeMessage || type.isColorWholeMessage() ? colorForLogType : "");
    }
}
