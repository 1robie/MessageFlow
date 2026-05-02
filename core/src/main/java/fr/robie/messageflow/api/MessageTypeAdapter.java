package fr.robie.messageflow.api;

import fr.robie.messageflow.model.MessageType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface MessageTypeAdapter {

    @NotNull
    MessageType messageType();

    @NotNull
    Map<String, Object> serialize();
}
