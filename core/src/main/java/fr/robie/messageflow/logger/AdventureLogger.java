package fr.robie.messageflow.logger;

import com.google.common.base.Throwables;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
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
public final class AdventureLogger extends Logger {

    /**
     * Constructs a new AdventureLogger.
     *
     * @param prefix           the log getPrefix
     * @param messageFormatter the Adventure message formatter to use
     */
    public AdventureLogger(@NotNull String prefix, @NotNull AdventureMessageFormatter<? extends Plugin> messageFormatter) {
        super(prefix, messageFormatter);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull String message, @NotNull Placeholder placeholders) {
        String fullMessage = this.prefixe(type) + message;
        this.messageFormatter.sendMessage(Bukkit.getConsoleSender(), fullMessage, false, placeholders);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull Message message, @NotNull Placeholder placeholders) {
        this.messageFormatter.sendMessage(message, type, Bukkit.getConsoleSender(), placeholders);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull Throwable throwable, @NotNull Message message, @NotNull Placeholder placeholders) {
        this.log(type, message, placeholders);
        String stackTrace = "\n<red>" + Throwables.getStackTraceAsString(throwable);
        this.messageFormatter.sendMessageWithoutPrefix(Bukkit.getConsoleSender(), stackTrace);
    }

    @Override
    protected void log(@NotNull LogType type, @NotNull Throwable throwable, @NotNull String message, @NotNull Placeholder placeholders) {
        String fullMessage = this.prefixe(type) + message + "\n<red>" + Throwables.getStackTraceAsString(throwable);
        this.messageFormatter.sendMessage(Bukkit.getConsoleSender(), fullMessage, false, placeholders);
    }


    @Override
    @NotNull
    protected String prefixe(@NotNull LogType type) {
        String colorForLogType = type.getAdventureColorCode();
        String typeName = (this.showTypeNames && type.isShowTypeName()) ? colorForLogType + "[" + type.name() + "] " : "";
        return typeName + "<reset>" + this.prefix + " " + (this.colorWholeMessage || type.isColorWholeMessage() ? colorForLogType : "");
    }
}
