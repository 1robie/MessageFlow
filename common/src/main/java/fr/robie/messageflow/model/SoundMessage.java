package fr.robie.messageflow.model;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A message adapter for sound messages played to players.
 * Contains sound, category, volume, and pitch settings.
 */
public final class SoundMessage extends MessageTypeAdapter {
    private final Sound sound;
    private final SoundCategory category;
    private final float volume;
    private final float pitch;

    public SoundMessage(@NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {
        this(sound, category, volume, pitch, false, false, false);
    }

    public SoundMessage(@NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch,
                        boolean broadcast, boolean sendToConsole, boolean excludeSenders) {
        super(broadcast, sendToConsole, excludeSenders);
        this.sound = sound;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.SOUND;
    }

    public @NotNull Sound sound() {
        return this.sound;
    }

    public @NotNull SoundCategory category() {
        return this.category;
    }

    public float volume() {
        return this.volume;
    }

    public float pitch() {
        return this.pitch;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("sound", this.sound.key().toString());
        map.put("category", this.category.name());
        map.put("volume", this.volume);
        map.put("pitch", this.pitch);
        this.serializeSettings(map);
        return map;
    }

    public static SoundMessage deserialize(Map<String, Object> data) {
        String soundName = (String) data.get("sound");
        if (soundName == null) {
            throw new IllegalArgumentException("Missing 'sound' field for SoundMessage");
        }
        Sound sound;
        try {
            sound = Registry.SOUNDS.get(NamespacedKey.fromString(soundName));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid sound name: " + soundName, e);
        }
        SoundCategory category = SoundCategory.MASTER;
        if (data.containsKey("category")) {
            String categoryName = (String) data.get("category");
            try {
                category = SoundCategory.valueOf(categoryName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid sound category: " + categoryName, e);
            }
        }
        float volume = ((Number) data.getOrDefault("volume", 1.0)).floatValue();
        float pitch = ((Number) data.getOrDefault("pitch", 1.0)).floatValue();

        boolean[] settings = parseSettings(data);

        if (sound != null) {
            return new SoundMessage(sound, category, volume, pitch, settings[0], settings[1], settings[2]);
        }
        Logger.warn("Sound '%sound%' not found. Defaulting to ENTITY_EXPERIENCE_ORB_PICKUP.", Placeholder.of("sound", soundName));
        return new SoundMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, category, volume, pitch, settings[0], settings[1], settings[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        SoundMessage that = (SoundMessage) o;
        return this.broadcast() == that.broadcast() &&
                this.sendToConsole() == that.sendToConsole() &&
                this.excludeSenders() == that.excludeSenders() &&
                Float.compare(that.volume, this.volume) == 0 &&
                Float.compare(that.pitch, this.pitch) == 0 &&
                Objects.equals(this.sound, that.sound) &&
                this.category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sound, this.category, this.volume, this.pitch, this.broadcast(), this.sendToConsole(), this.excludeSenders());
    }

    @Override
    public String toString() {
        return "SoundMessage[" +
                "sound=" + this.sound +
                ", category=" + this.category +
                ", volume=" + this.volume +
                ", pitch=" + this.pitch +
                ", broadcast=" + this.broadcast() +
                ", sendToConsole=" + this.sendToConsole() +
                ", excludeSenders=" + this.excludeSenders() +
                ']';
    }
}
