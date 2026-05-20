package fr.robie.messageflow.api;

import fr.robie.messageflow.model.MessageType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface representing a message adapter for a specific message type.
 */
public interface MessageTypeAdapter {

    /**
     * Retrieves the specific type of this message handled by the adapter. This type determines
     * how the message will be serialized or presented to the system or end-user.
     *
     * @return the type of the message represented as {@link MessageType}
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
