package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A message adapter for boss bar messages using the Adventure API.
 * Contains title, color, overlay, flags, duration, and progress settings.
 *
 * @param title    the boss bar text
 * @param color    the boss bar color
 * @param overlay  the boss bar overlay style
 * @param flags    boss bar flags
 * @param duration the display duration in milliseconds
 * @param progress the progress value (0.0 to 1.0)
 */
public record AdventureBossBarMessage(@NotNull String title, @NotNull BossBar.Color color, @NotNull BossBar.Overlay overlay,
                                  @NotNull Set<BossBar.Flag> flags, long duration,
                                  float progress) implements MessageTypeAdapter {

    public AdventureBossBarMessage(@NotNull String title, @NotNull BossBar.Color color, @NotNull BossBar.Overlay overlay, @NotNull Set<BossBar.Flag> flags, long duration, float progress) {
        Preconditions.checkNotNull(title, "Title cannot be null");
        Preconditions.checkNotNull(color, "Color cannot be null");
        Preconditions.checkNotNull(overlay, "Overlay cannot be null");
        Preconditions.checkNotNull(flags, "Flags cannot be null");
        this.title = title;
        this.color = color;
        this.overlay = overlay;
        this.flags = flags;
        this.duration = duration;
        this.progress = progress;
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
    public @NonNull Map<String, Object> serialize() {
        return Map.of(
                "title", this.title,
                "color", this.color.name(),
                "overlay", this.overlay.name(),
                "flags", this.flags.stream().map(BossBar.Flag::name).toList(),
                "duration", this.duration,
                "progress", this.progress
        );
    }

    /**
     * Deserializes an AdventureBossBarMessage from a YAML map.
     *
     * @param map the map containing boss bar data
     * @return the deserialized boss bar message
     */
    public static AdventureBossBarMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(((String) map.getOrDefault("color", "PINK")).toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.PINK;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(((String) map.getOrDefault("overlay", "PROGRESS")).toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        Set<BossBar.Flag> flags;
        Object rawFlags = map.get("flags");
        if (rawFlags instanceof List<?> list && !list.isEmpty()) {
            flags = list.stream()
                    .filter(f -> f instanceof String)
                    .map(f -> {
                        try {
                            return BossBar.Flag.valueOf(((String) f).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            if (flags.isEmpty()) {
                flags = null;
            }
        } else {
            flags = Collections.emptySet();
        }

        long duration = ((Number) map.getOrDefault("duration", 100L)).longValue();
        float progress = ((Number) map.getOrDefault("progress", 1.0f)).floatValue();
        return new AdventureBossBarMessage(title, color, overlay, flags == null ? Collections.emptySet() : flags, duration, progress);
    }
}
