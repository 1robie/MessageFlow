package fr.robie.messageflow.model;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record SoundMessage(
        @NotNull Sound sound,
        @NotNull SoundCategory category,
        float volume,
        float pitch
) implements MessageTypeAdapter {

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.SOUND;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of();
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
        SoundCategory category = SoundCategory.MASTER; // Default category
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
        if (sound != null) {
            return new SoundMessage(sound, category, volume, pitch);
        }
        Logger.warn("Sound '%sound%' not found. Defaulting to ENTITY_EXPERIENCE_ORB_PICKUP.", Placeholder.of("sound", soundName));
        return new SoundMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, category, volume, pitch);
    }
}
