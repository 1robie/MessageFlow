package fr.robie.messageflow.model;

import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public record SimpleMessage(@NotNull MessageType messageType,
                            @NotNull List<String> messages) implements MessageTypeAdapter {

    @Override
    public @NonNull Map<String, Object> serialize() {
        if (this.messages.size() == 1) {
            return Map.of("message", this.messages.getFirst());
        } else {
            return Map.of("messages", this.messages);
        }
    }

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
