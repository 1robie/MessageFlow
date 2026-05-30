package fr.robie.exempleplugin;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageSettings;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import fr.robie.messageflow.model.TitleMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public enum ExampleMessages implements Message {
    PREFIX("getPrefix", List.of(new SimpleMessage(MessageType.WITHOUT_PREFIX, List.of("&7[&bExample&7]&r ")))),
    HELLO("hello", List.of(Message.chat("&aHello from MessageFlow!"))),
    ONLY_FRENCH("only-french", List.of(Message.chat("&c(You are reading the default value)"))),
    IMPORTANT_NOTICE("important-notice", List.of(new TitleMessage("&eImportant!", "&fPlease read this notice.", 10, 70, 20)), MessageSettings.whitelist(MessageType.TITLE, MessageType.ACTION_BAR));

    private final String key;
    private final List<? extends MessageTypeAdapter> defaults;
    private final MessageSettings staticSettings;
    private List<? extends MessageTypeAdapter> loaded;
    private MessageSettings loadedSettings;

    ExampleMessages(String key, List<? extends MessageTypeAdapter> defaults) {
        this(key, defaults, MessageSettings.DEFAULT);
    }

    ExampleMessages(String key, List<? extends MessageTypeAdapter> defaults, MessageSettings settings) {
        this.key = key;
        this.defaults = defaults;
        this.staticSettings = settings;
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

    @Override
    public void setSettings(@NotNull MessageSettings settings) {
        this.loadedSettings = settings;
    }

    @Override
    public @NotNull MessageSettings settings() {
        return this.loadedSettings != null ? this.loadedSettings : this.staticSettings;
    }
}

