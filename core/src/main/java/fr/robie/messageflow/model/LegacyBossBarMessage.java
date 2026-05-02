package fr.robie.messageflow.model;

import fr.robie.messageflow.api.MessageTypeAdapter;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record LegacyBossBarMessage(
        @NotNull String title, @NotNull BarColor color, @NotNull BarStyle style,
        @Nullable BarFlag[] flags, long duration, float progress
        ) implements MessageTypeAdapter {
    @Override
    public @NotNull MessageType messageType() {
        return MessageType.BOSS_BAR;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
                "title", this.title,
                "color", this.color.name(),
                "style", this.style.name(),
                "flags", this.flags.length > 0 ? Set.of(this.flags).stream().map(BarFlag::name).toList() : null,
                "duration", this.duration,
                "progress", this.progress
        );
    }

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
