package fr.robie.messageflow.model;

import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * A simple message adapter for chat, action bar, and broadcast message types.
 * Contains one or more text lines and a message type.
 *
 * @param messageType the type of this message
 * @param messages    the list of text lines to display
 */
public record SimpleMessage(@NotNull MessageType messageType,
                            @NotNull List<String> messages) implements MessageTypeAdapter {

    /**
     * Serializes this message to a map for YAML storage.
     * Uses "message" key for single-line messages and "messages" for multi-line.
     *
     * @return the serialized map
     */
    @Override
    public @NonNull Map<String, Object> serialize() {
        if (this.messages.size() == 1) {
            return Map.of("message", this.messages.getFirst());
        } else {
            return Map.of("messages", this.messages);
        }
    }

    /**
     * Deserializes a SimpleMessage from a YAML map.
     *
     * @param messageType the expected message type
     * @param map         the map containing message data
     * @return the deserialized message, or null if parsing failed
     */
    @Nullable
    public static SimpleMessage deserialize(MessageType messageType, Map<String, Object> map) {
        List<String> messages;
        if (map.containsKey("message")) {
            messages = List.of((String) map.get("message"));
        } else {
            messages = (List<String>) map.get("messages");
        }
        return messages != null ? new SimpleMessage(messageType, messages) : null;
    }
}
