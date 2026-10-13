package fr.robie.messageflow.logger;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Logger implementation that uses Adventure (MiniMessage) for formatting.
 * <p>
 * This logger is suitable for modern Minecraft versions (1.16+) that support
 * rich text components and MiniMessage formatting.
 * <p>
 * On Paper, lines go to the plugin's {@code ComponentLogger}: the server names the plugin and keeps the level, so
 * warnings and errors are logged as such (log files, panels and alerts see them), in color in the console.
 * Elsewhere, or with {@code logger.component-logger: false}, lines are sent to the console sender with the
 * configured prefix.
 */
public final class AdventureLogger extends Logger {
    private final AdventureMessageFormatter<?> adventure;
    private final @Nullable ComponentLogger componentLogger;

    /**
     * Constructs a new AdventureLogger.
     *
     * @param prefix           the log getPrefix
     * @param messageFormatter the Adventure message formatter to use
     */
    public AdventureLogger(@NotNull String prefix, @NotNull AdventureMessageFormatter<? extends Plugin> messageFormatter) {
        super(prefix, messageFormatter);
        this.adventure = messageFormatter;
        this.componentLogger = ConfigurationManager.Setting.LOGGER_COMPONENT_LOGGER.<Boolean>getValue()
                ? componentLogger(messageFormatter.getPlugin()) : null;
    }

    private static @Nullable ComponentLogger componentLogger(@NotNull Plugin plugin) {
        try {
            return plugin.getComponentLogger();
        } catch (LinkageError | RuntimeException e) {
            return null; // not Paper
        }
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull String message, @NotNull Placeholder placeholders) {
        ComponentLogger out = this.componentLogger;
        if (out == null) {
            super.log(context, type, message, placeholders);
            return;
        }
        write(out, type, this.line(context, type, message, placeholders), null);
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull Throwable throwable, @NotNull String message, @NotNull Placeholder placeholders) {
        ComponentLogger out = this.componentLogger;
        if (out == null) {
            super.log(context, type, throwable, message, placeholders);
            return;
        }
        write(out, type, this.line(context, type, message, placeholders), throwable);
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull Message message, @NotNull Placeholder placeholders) {
        ComponentLogger out = this.componentLogger;
        if (out == null) {
            super.log(context, type, message, placeholders);
            return;
        }
        for (String line : chatLines(message)) {
            write(out, type, this.line(context, type, line, placeholders), null);
        }
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull Throwable throwable, @NotNull Message message, @NotNull Placeholder placeholders) {
        ComponentLogger out = this.componentLogger;
        if (out == null) {
            super.log(context, type, throwable, message, placeholders);
            return;
        }
        write(out, type, this.line(context, type, String.join("\n", chatLines(message)), placeholders), throwable);
    }

    /**
     * A line for the component logger: no prefix (the server names the plugin) and no level name, except
     * {@code [DEBUG]} (logged as info), in the level's color when it colors the whole message.
     */
    private @NotNull Component line(@Nullable String context, @NotNull LogType type, @NotNull String message, @NotNull Placeholder placeholders) {
        String color = type.getAdventureColorCode();
        String typeName = type == LogType.DEBUG && this.showTypeNames && type.isShowTypeName() ? color + "[DEBUG]<reset> " : "";
        String whole = this.colorWholeMessage || type.isColorWholeMessage() ? color : "";
        return this.adventure.format(typeName + whole + (context != null ? context + " " : "") + message, null, placeholders);
    }

    private static void write(@NotNull ComponentLogger out, @NotNull LogType type, @NotNull Component line, @Nullable Throwable throwable) {
        switch (type) {
            case ERROR -> {
                if (throwable == null) out.error(line);
                else out.error(line, throwable);
            }
            case WARNING -> {
                if (throwable == null) out.warn(line);
                else out.warn(line, throwable);
            }
            case INFO, DEBUG -> {
                if (throwable == null) out.info(line);
                else out.info(line, throwable);
            }
        }
    }

    private static @NotNull List<String> chatLines(@NotNull Message message) {
        List<? extends MessageTypeAdapter> adapters = message.loaded().isEmpty() ? message.defaults() : message.loaded();
        List<String> lines = new ArrayList<>();
        for (MessageTypeAdapter adapter : adapters) {
            if (adapter.messageType() == MessageType.TCHAT && adapter instanceof SimpleMessage simple) {
                lines.addAll(simple.messages());
            }
        }
        return lines;
    }

    @Override
    @NotNull
    protected String getErrorColor() {
        return "<red>";
    }

    @Override
    @NotNull
    protected String prefixe(@NotNull LogType type) {
        String colorForLogType = type.getAdventureColorCode();
        String typeName = (this.showTypeNames && type.isShowTypeName()) ? colorForLogType + "[" + type.name() + "] " : "";
        return typeName + "<reset>" + this.prefix + " " + (this.colorWholeMessage || type.isColorWholeMessage() ? colorForLogType : "");
    }
}
