package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A message adapter for boss bar messages using the legacy Bukkit API.
 * Contains title, color, style, flags, duration, and progress settings.
 *
 * @param title    the boss bar text
 * @param color    the boss bar color
 * @param style    the boss bar style
 * @param flags    optional boss bar flags
 * @param duration the display duration in milliseconds
 * @param progress the progress value (0.0 to 1.0)
 */
public record LegacyBossBarMessage(
        @NotNull String title, @NotNull BarColor color, @NotNull BarStyle style,
        @Nullable BarFlag[] flags, long duration, float progress
        ) implements MessageTypeAdapter {

    public LegacyBossBarMessage {
        Preconditions.checkNotNull(title, "Title cannot be null");
        Preconditions.checkNotNull(color, "Color cannot be null");
        Preconditions.checkNotNull(style, "Style cannot be null");
    }

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.BOSS_BAR;
    }

    /**
     * Serializes this boss bar message to a map for YAML storage.
     *
     * @return the serialized map containing boss bar configuration
     */
    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
                "title", this.title,
                "color", this.color.name(),
                "style", this.style.name(),
                "flags", (this.flags != null && this.flags.length > 0) ? Set.of(this.flags).stream().map(BarFlag::name).toList() : List.of(),
                "duration", this.duration,
                "progress", this.progress
        );
    }

    /**
     * Deserializes a LegacyBossBarMessage from a YAML map.
     *
     * @param map the map containing boss bar data
     * @return the deserialized boss bar message
     */
    public static LegacyBossBarMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");

        BarColor color;
        try {
            color = BarColor.valueOf(((String) map.getOrDefault("color", "PINK")).toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BarColor.PINK;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(((String) map.getOrDefault("style", "SOLID")).toUpperCase());
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        BarFlag[] flags = null;
        Object rawFlags = map.get("flags");
        if (rawFlags instanceof Collection<?> collection && !collection.isEmpty()) {
            flags = collection.stream()
                    .filter(f -> f instanceof String)
                    .map(f -> {
                        try {
                            return BarFlag.valueOf(((String) f).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(BarFlag[]::new);
        }

        long duration = ((Number) map.getOrDefault("duration", 100L)).longValue();
        float progress = ((Number) map.getOrDefault("progress", 1.0f)).floatValue();
        return new LegacyBossBarMessage(title, color, style, flags, duration, progress);
    }
}
