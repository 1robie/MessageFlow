package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

/**
 * A message adapter for boss bar messages using the Adventure API.
 * Contains title, color, overlay, flags, duration, and progress settings.
 */
public final class AdventureBossBarMessage extends MessageTypeAdapter {
    private final String title;
    private final BossBar.Color color;
    private final BossBar.Overlay overlay;
    private final Set<BossBar.Flag> flags;
    private final long duration;
    private final float progress;

    public AdventureBossBarMessage(@NotNull String title, @NotNull BossBar.Color color,
                                   @NotNull BossBar.Overlay overlay,
                                   @NotNull Set<BossBar.Flag> flags, long duration,
                                   float progress) {
        this(title, color, overlay, flags, duration, progress, false, false, false);
    }

    public AdventureBossBarMessage(@NotNull String title, @NotNull BossBar.Color color,
                                   @NotNull BossBar.Overlay overlay,
                                   @NotNull Set<BossBar.Flag> flags, long duration,
                                   float progress, boolean broadcast, boolean sendToConsole,
                                   boolean excludeSenders) {
        super(broadcast, sendToConsole, excludeSenders);
        this.title = Preconditions.checkNotNull(title, "Title cannot be null");
        this.color = Preconditions.checkNotNull(color, "Color cannot be null");
        this.overlay = Preconditions.checkNotNull(overlay, "Overlay cannot be null");
        this.flags = Preconditions.checkNotNull(flags, "Flags cannot be null");
        this.duration = duration;
        this.progress = progress;
    }

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.BOSS_BAR;
    }

    public @NotNull String title() {
        return this.title;
    }

    public @NotNull BossBar.Color color() {
        return this.color;
    }

    public @NotNull BossBar.Overlay overlay() {
        return this.overlay;
    }

    public @NotNull Set<BossBar.Flag> flags() {
        return this.flags;
    }

    public long duration() {
        return this.duration;
    }

    public float progress() {
        return this.progress;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", this.title);
        map.put("color", this.color.name());
        map.put("overlay", this.overlay.name());
        map.put("flags", this.flags.stream().map(BossBar.Flag::name).toList());
        map.put("duration", this.duration);
        map.put("progress", this.progress);
        this.serializeSettings(map);
        return map;
    }

    public static AdventureBossBarMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(((String) map.getOrDefault("color", "PINK")).toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid boss bar color value: %value%. Valid values are %valid_values%. Defaulting to PINK.", Placeholder.of("value", String.valueOf(map.get("color")), "valid_values", Stream.of(BossBar.Color.values()).map(BossBar.Color::name).toList().toString()));
            color = BossBar.Color.PINK;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(((String) map.getOrDefault("overlay", "PROGRESS")).toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid boss bar overlay value: %value%. Valid values are %valid_values%. Defaulting to PROGRESS.", Placeholder.of("value", String.valueOf(map.get("overlay")), "valid_values", Stream.of(BossBar.Overlay.values()).map(BossBar.Overlay::name).toList().toString()));
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

        boolean[] settings = parseSettings(map);
        return new AdventureBossBarMessage(title, color, overlay, flags == null ? Collections.emptySet() : flags, duration, progress, settings[0], settings[1], settings[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        AdventureBossBarMessage that = (AdventureBossBarMessage) o;
        return this.broadcast() == that.broadcast() &&
                this.sendToConsole() == that.sendToConsole() &&
                this.excludeSenders() == that.excludeSenders() &&
                this.duration == that.duration &&
                Float.compare(that.progress, this.progress) == 0 &&
                Objects.equals(this.title, that.title) &&
                this.color == that.color &&
                this.overlay == that.overlay &&
                Objects.equals(this.flags, that.flags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.title, this.color, this.overlay, this.flags, this.duration, this.progress, this.broadcast(), this.sendToConsole(), this.excludeSenders());
    }

    @Override
    public String toString() {
        return "AdventureBossBarMessage[" +
                "title='" + this.title + '\'' +
                ", color=" + this.color +
                ", overlay=" + this.overlay +
                ", flags=" + this.flags +
                ", duration=" + this.duration +
                ", progress=" + this.progress +
                ", broadcast=" + this.broadcast() +
                ", sendToConsole=" + this.sendToConsole() +
                ", excludeSenders=" + this.excludeSenders() +
                ']';
    }
}
