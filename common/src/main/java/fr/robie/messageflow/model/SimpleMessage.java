package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple message adapter for chat, action bar, and broadcast message types.
 * Contains one or more text lines and a message type.
 */
public final class SimpleMessage extends MessageTypeAdapter {
    private final MessageType messageType;
    private final List<String> messages;

    public SimpleMessage(@NotNull MessageType messageType, @NotNull List<String> messages) {
        this(messageType, messages, false, false, false);
    }

    public SimpleMessage(@NotNull MessageType messageType, @NotNull List<String> messages,
                         boolean broadcast, boolean sendToConsole, boolean excludeSenders) {
        super(broadcast, sendToConsole, excludeSenders);
        this.messageType = Preconditions.checkNotNull(messageType, "Message type cannot be null");
        this.messages = Preconditions.checkNotNull(messages, "Messages list cannot be null");
    }

    @Override
    public @NotNull MessageType messageType() {
        return this.messageType;
    }

    public @NotNull List<String> messages() {
        return this.messages;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        if (this.messages.size() == 1) {
            map.put("message", this.messages.getFirst());
        } else {
            map.put("messages", this.messages);
        }
        this.serializeSettings(map);
        return map;
    }

    @Nullable
    public static SimpleMessage deserialize(MessageType messageType, Map<String, Object> map) {
        List<String> messages;
        if (map.containsKey("message")) {
            messages = List.of((String) map.get("message"));
        } else if (map.containsKey("messages")) {
            @SuppressWarnings("unchecked")
            List<String> temp = (List<String>) map.get("messages");
            messages = temp;
        } else {
            messages = null;
        }

        if (messages == null) {
            return null;
        }

        boolean[] settings = parseSettings(map);
        return new SimpleMessage(messageType, messages, settings[0], settings[1], settings[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        SimpleMessage that = (SimpleMessage) o;
        return this.broadcast() == that.broadcast() &&
                this.sendToConsole() == that.sendToConsole() &&
                this.excludeSenders() == that.excludeSenders() &&
                this.messageType == that.messageType &&
                Objects.equals(this.messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.messageType, this.messages, this.broadcast(), this.sendToConsole(), this.excludeSenders());
    }

    @Override
    public String toString() {
        return "SimpleMessage[" +
                "messageType=" + this.messageType +
                ", messages=" + this.messages +
                ", broadcast=" + this.broadcast() +
                ", sendToConsole=" + this.sendToConsole() +
                ", excludeSenders=" + this.excludeSenders() +
                ']';
    }
}
