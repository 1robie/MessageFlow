package fr.robie.messageflow.message;

import fr.robie.messageflow.MessageType;
import fr.robie.messageflow.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public record TitleMessage(@NotNull String title, @Nullable String subtitle, int fadeIn, int stay,
                           int fadeOut) implements MessageTypeAdapter {

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.TITLE;
    }

    @Override
    public @NonNull Map<String, Object> serialize() {
        return Map.of(
                "title", this.title,
                "subtitle", this.subtitle != null ? this.subtitle : "",
                "fade-in", this.fadeIn,
                "stay", this.stay,
                "fade-out", this.fadeOut
        );
    }

    public static TitleMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");
        String subtitle = (String) map.getOrDefault("subtitle", null);
        int fadeIn = ((Number) map.getOrDefault("fade-in", 10)).intValue();
        int stay = ((Number) map.getOrDefault("stay", 70)).intValue();
        int fadeOut = ((Number) map.getOrDefault("fade-out", 20)).intValue();
        return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
    }

}
