package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.IMessageBuilder;
import fr.robie.messageflow.api.IMessageManager;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.api.PlaceholderValue;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of {@link IMessageBuilder} that aggregates message components
 * and sends them using the manager's formatter.
 */
public final class MessageBuilder implements IMessageBuilder {
    private final MessageFormatter<?, ?> formatter;
    private final List<MessageTypeAdapter> adapters = new ArrayList<>();
    private final Placeholder.Builder placeholders = Placeholder.builder();
    private boolean usePrefix = true;

    public MessageBuilder(@NotNull IMessageManager<?, ?> manager) {
        this.formatter = manager.formatter();
    }

    @Override
    public @NotNull IMessageBuilder chat(@NotNull String... lines) {
        this.adapters.add(new SimpleMessage(MessageType.TCHAT, List.of(lines)));
        return this;
    }

    @Override
    public @NotNull IMessageBuilder actionBar(@NotNull String line) {
        this.adapters.add(new SimpleMessage(MessageType.ACTION_BAR, List.of(line)));
        return this;
    }

    @Override
    public @NotNull IMessageBuilder title(@NotNull String title, @NotNull String subtitle, int fadeIn, int stay, int fadeOut) {
        this.adapters.add(new TitleMessage(title, subtitle, fadeIn, stay, fadeOut));
        return this;
    }

    @Override
    public @NotNull IMessageBuilder title(@NotNull String title, @NotNull String subtitle) {
        return this.title(title, subtitle, 10, 70, 20);
    }

    @Override
    public @NotNull IMessageBuilder sound(@NotNull String sound, float volume, float pitch) {
        NamespacedKey key = NamespacedKey.fromString(sound);
        if (key == null) {
            key = NamespacedKey.minecraft(sound);
        }
        Sound bukkitSound = Registry.SOUNDS.get(key);
        if (bukkitSound == null) {
            bukkitSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        this.adapters.add(new SoundMessage(bukkitSound, SoundCategory.MASTER, volume, pitch));
        return this;
    }

    @Override
    public @NotNull IMessageBuilder sound(@NotNull String sound) {
        return this.sound(sound, 1.0f, 1.0f);
    }

    @Override
    public @NotNull IMessageBuilder placeholder(@NotNull String key, @NotNull String value) {
        this.placeholders.register(key, value);
        return this;
    }

    @Override
    public @NotNull IMessageBuilder placeholder(@NotNull String key, @NotNull Supplier<String> value) {
        this.placeholders.register(key, value);
        return this;
    }

    @Override
    public @NotNull IMessageBuilder placeholder(@NotNull String key, @NotNull Function<Player, String> value) {
        this.placeholders.register(key, value);
        return this;
    }

    @Override
    public @NotNull IMessageBuilder placeholders(@NotNull Placeholder placeholders) {
        placeholders.getMap().forEach((key, value) -> {
            if (value instanceof PlaceholderValue.StaticValue(String value1)) {
                this.placeholders.register(key, value1);
            } else if (value instanceof PlaceholderValue.DynamicValue(Supplier<String> supplier)) {
                this.placeholders.register(key, supplier);
            } else if (value instanceof PlaceholderValue.PlayerValue(Function<Player, String> function)) {
                this.placeholders.register(key, function);
            }
        });
        return this;
    }

    @Override
    public @NotNull IMessageBuilder prefix(boolean prefix) {
        this.usePrefix = prefix;
        return this;
    }

    @Override
    public void send(@NotNull CommandSender sender) {
        this.send(List.of(sender));
    }

    @Override
    public void send(@NotNull Collection<? extends CommandSender> senders) {
        if (this.adapters.isEmpty() || senders.isEmpty()) {
            return;
        }

        Message transientMessage = new Message() {
            @Override
            public @NotNull String key() {
                return "__transient__";
            }

            @Override
            public @NotNull List<? extends MessageTypeAdapter> defaults() {
                return MessageBuilder.this.adapters;
            }

            @Override
            public @NotNull List<? extends MessageTypeAdapter> loaded() {
                return MessageBuilder.this.adapters;
            }

            @Override
            public void setLoaded(@NotNull List<? extends MessageTypeAdapter> loaded) {
            }
        };

        this.formatter.sendMessage(transientMessage, senders, this.usePrefix, this.placeholders.build());
    }

    @Override
    public void broadcast() {
        this.send(Bukkit.getOnlinePlayers());
    }
}
