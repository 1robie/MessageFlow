package fr.robie.messageflow.model;

import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Message {
    /**
     * Config key for this message (ex: {@code "errors.no-permission"}).
     */
    @NotNull String key();

    /**
     * Default messages written when the config key is missing.
     */
    @NotNull List<? extends MessageTypeAdapter> defaults();

    @NotNull List<? extends MessageTypeAdapter> loaded();

    void setLoaded(@NotNull List<? extends MessageTypeAdapter> loaded);

    /**
     * Sets the loaded settings for this message.
     *
     * @param settings the loaded settings
     */
    default void setSettings(@NotNull MessageSettings settings) {
    }

    /**
     * @return The message settings for this message.
     */
    default @NotNull MessageSettings settings() {
        return MessageSettings.DEFAULT;
    }

    /**
     * Convenience: returns the first text line found in the loaded messages.
     * This mirrors the common "classic message first line" usage.
     */
    default @NotNull String firstLine() {
        List<? extends MessageTypeAdapter> adapters = this.loaded();
        if (adapters.isEmpty()) {
            adapters = this.defaults();
        }
        if (adapters.isEmpty()) {
            return "";
        }

        for (MessageTypeAdapter adapter : adapters) {
            if (adapter instanceof SimpleMessage classic) {
                List<String> lines = classic.messages();
                if (!lines.isEmpty()) {
                    return lines.getFirst();
                }
            }
        }
        return "";
    }

    /**
     * Convenience for the common chat case.
     */
    static @NotNull SimpleMessage chat(@NotNull String message) {
        return new SimpleMessage(MessageType.TCHAT, List.of(message));
    }
}
