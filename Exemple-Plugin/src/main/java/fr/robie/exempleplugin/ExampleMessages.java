package fr.robie.exempleplugin;

import fr.robie.messageflow.Message;
import fr.robie.messageflow.MessageTypeAdapter;
import fr.robie.messageflow.message.SimpleMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ExampleMessages implements Message {
    PREFIX("prefix", List.of(new SimpleMessage(fr.robie.messageflow.MessageType.WITHOUT_PREFIX, List.of("&7[&bExample&7]&r ")))),
    HELLO("hello", List.of(Message.chat("&aHello from MessageFlow!"))),
    ONLY_FRENCH("only-french", List.of(Message.chat("&c(You are reading the default value)")));

    private final String key;
    private final List<? extends MessageTypeAdapter> defaults;
    private List<? extends MessageTypeAdapter> loaded;

    ExampleMessages(String key, List<? extends MessageTypeAdapter> defaults) {
        this.key = key;
        this.defaults = defaults;
    }

    @Override
    public @NotNull String key() {
        return this.key;
    }

    @Override
    public @NotNull List<? extends MessageTypeAdapter> defaults() {
        return this.defaults;
    }

    @Override
    public @NotNull List<? extends MessageTypeAdapter> loaded() {
        return this.loaded != null ? this.loaded : this.defaults;
    }

    @Override
    public void setLoaded(@NotNull List<? extends MessageTypeAdapter> loaded) {
        this.loaded = loaded;
    }
}

