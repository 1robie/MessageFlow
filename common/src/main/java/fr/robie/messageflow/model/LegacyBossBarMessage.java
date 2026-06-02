package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * A message adapter for boss bar messages using the legacy Bukkit API.
 * Contains title, color, style, flags, duration, and progress settings.
 */
public final class LegacyBossBarMessage extends MessageTypeAdapter {
    private final String title;
    private final BarColor color;
    private final BarStyle style;
    private final BarFlag[] flags;
    private final long duration;
    private final float progress;

    public LegacyBossBarMessage(
            @NotNull String title, @NotNull BarColor color, @NotNull BarStyle style,
            @Nullable BarFlag[] flags, long duration, float progress
    ) {
        this(title, color, style, flags, duration, progress, false, false, false);
    }

    public LegacyBossBarMessage(
            @NotNull String title, @NotNull BarColor color, @NotNull BarStyle style,
            @Nullable BarFlag[] flags, long duration, float progress,
            boolean broadcast, boolean sendToConsole, boolean excludeSenders
    ) {
        super(broadcast, sendToConsole, excludeSenders);
        this.title = Preconditions.checkNotNull(title, "Title cannot be null");
        this.color = Preconditions.checkNotNull(color, "Color cannot be null");
        this.style = Preconditions.checkNotNull(style, "Style cannot be null");
        this.flags = flags;
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

    public @NotNull BarColor color() {
        return this.color;
    }

    public @NotNull BarStyle style() {
        return this.style;
    }

    public @Nullable BarFlag[] flags() {
        return this.flags;
    }

    public long duration() {
        return this.duration;
    }

    public float progress() {
        return this.progress;
    }

    /**
     * Serializes this boss bar message to a map for YAML storage.
     *
     * @return the serialized map containing boss bar configuration
     */
    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", this.title);
        map.put("color", this.color.name());
        map.put("style", this.style.name());
        map.put("flags", (this.flags != null && this.flags.length > 0) ? Set.of(this.flags).stream().map(BarFlag::name).toList() : List.of());
        map.put("duration", this.duration);
        map.put("progress", this.progress);
        this.serializeSettings(map);
        return map;
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
            color = BarColor.valueOf(((String) map.getOrDefault("color", "PINK")).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid boss bar color value: %value%. Valid values are %valid_values%. Defaulting to PINK.", Placeholder.of("value", String.valueOf(map.get("color")), "valid_values", Stream.of(BarColor.values()).map(BarColor::name).toList().toString()));
            color = BarColor.PINK;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(((String) map.getOrDefault("style", "SOLID")).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid boss bar style value: %value%. Valid values are %valid_values%. Defaulting to SOLID.", Placeholder.of("value", String.valueOf(map.get("style")), "valid_values", Stream.of(BarStyle.values()).map(BarStyle::name).toList().toString()));
            style = BarStyle.SOLID;
        }

        BarFlag[] flags = null;
        Object rawFlags = map.get("flags");
        if (rawFlags instanceof Collection<?> collection && !collection.isEmpty()) {
            flags = collection.stream()
                    .filter(f -> f instanceof String)
                    .map(f -> {
                        try {
                            return BarFlag.valueOf(((String) f).toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(BarFlag[]::new);
        }

        long duration = ((Number) map.getOrDefault("duration", 100L)).longValue();
        float progress = ((Number) map.getOrDefault("progress", 1.0f)).floatValue();

        boolean[] settings = parseSettings(map);
        return new LegacyBossBarMessage(title, color, style, flags, duration, progress, settings[0], settings[1], settings[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        LegacyBossBarMessage that = (LegacyBossBarMessage) o;
        return this.broadcast() == that.broadcast() &&
                this.sendToConsole() == that.sendToConsole() &&
                this.excludeSenders() == that.excludeSenders() &&
                this.duration == that.duration &&
                Float.compare(that.progress, this.progress) == 0 &&
                Objects.equals(this.title, that.title) &&
                this.color == that.color &&
                this.style == that.style &&
                Arrays.equals(this.flags, that.flags);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(this.title, this.color, this.style, this.duration, this.progress, this.broadcast(), this.sendToConsole(), this.excludeSenders());
        result = 31 * result + Arrays.hashCode(this.flags);
        return result;
    }

    @Override
    public String toString() {
        return "LegacyBossBarMessage[" +
                "title='" + this.title + '\'' +
                ", color=" + this.color +
                ", style=" + this.style +
                ", flags=" + Arrays.toString(this.flags) +
                ", duration=" + this.duration +
                ", progress=" + this.progress +
                ", broadcast=" + this.broadcast() +
                ", sendToConsole=" + this.sendToConsole() +
                ", excludeSenders=" + this.excludeSenders() +
                ']';
    }
}
