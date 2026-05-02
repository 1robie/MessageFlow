package fr.robie.messageflow.api;

import fr.robie.messageflow.model.MessageType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface representing a message adapter for a specific message type.
 */
public interface MessageTypeAdapter {

    /**
     * Gets the type of the message.
     *
     * @return the message type
     */
    @NotNull
    MessageType messageType();

    /**
     * Serializes the message into a map of values for storage.
     *
     * @return the serialized message map
     */
    @NotNull
    Map<String, Object> serialize();
}
