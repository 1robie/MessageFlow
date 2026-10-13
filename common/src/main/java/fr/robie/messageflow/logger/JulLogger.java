package fr.robie.messageflow.logger;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import fr.robie.messageflow.util.PlatformType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes plain lines to a {@code java.util.logging} logger: placeholders are replaced, MiniMessage tags removed,
 * and an exception is attached to its record. Used without a {@code MessageManager}: before one exists, after it
 * is closed, and in unit tests ({@link Logger#jul(java.util.logging.Logger)}).
 */
final class JulLogger extends Logger {

    JulLogger(@NotNull java.util.logging.Logger jul) {
        super(jul);
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull String message, @NotNull Placeholder placeholders) {
        this.jul.log(level(type), this.line(context, message, placeholders));
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull Throwable throwable, @NotNull String message, @NotNull Placeholder placeholders) {
        this.jul.log(level(type), this.line(context, message, placeholders), throwable);
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull Message message, @NotNull Placeholder placeholders) {
        for (String line : this.lines(message)) {
            this.jul.log(level(type), this.line(context, line, placeholders));
        }
    }

    @Override
    protected void log(@Nullable String context, @NotNull LogType type, @NotNull Throwable throwable, @NotNull Message message, @NotNull Placeholder placeholders) {
        this.jul.log(level(type), this.line(context, String.join("\n", this.lines(message)), placeholders), throwable);
    }

    private @NotNull String line(@Nullable String context, @NotNull String message, @NotNull Placeholder placeholders) {
        // Tags are removed from the template only: placeholder values are kept as they are.
        String template = PlatformType.hasComponent() ? MiniMessage.miniMessage().stripTags(message) : message;
        String text = placeholders.parse(template);
        return context != null ? context + " " + text : text;
    }

    private @NotNull List<String> lines(@NotNull Message message) {
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
    protected @NotNull String getErrorColor() {
        return "";
    }

    @Override
    protected @NotNull String prefixe(@NotNull LogType type) {
        return "";
    }
}
