package fr.robie.messageflow;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface MessageTypeAdapter {

    @NotNull
    MessageType messageType();

    @NotNull
    Map<String, Object> serialize();
}
