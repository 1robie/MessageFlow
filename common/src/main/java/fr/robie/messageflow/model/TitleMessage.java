package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * A message adapter for title messages displayed in the center of the screen.
 * Contains a title, optional subtitle, and timing parameters.
 *
 * @param title    the main title text
 * @param subtitle the optional subtitle text
 * @param fadeIn   the fade-in time in ticks
 * @param stay     the stay time in ticks
 * @param fadeOut  the fade-out time in ticks
 */
public record TitleMessage(@NotNull String title, @Nullable String subtitle, int fadeIn, int stay,
                           int fadeOut) implements MessageTypeAdapter {

    public TitleMessage {
        Preconditions.checkNotNull(title, "Title cannot be null");
    }

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.TITLE;
    }

    /**
     * Serializes this title message to a map for YAML storage.
     *
     * @return the serialized map containing title, subtitle, and timing values
     */
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

    /**
     * Deserializes a TitleMessage from a YAML map.
     *
     * @param map the map containing title data
     * @return the deserialized title message
     */
    public static TitleMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");
        String subtitle = (String) map.getOrDefault("subtitle", null);
        int fadeIn = ((Number) map.getOrDefault("fade-in", 10)).intValue();
        int stay = ((Number) map.getOrDefault("stay", 70)).intValue();
        int fadeOut = ((Number) map.getOrDefault("fade-out", 20)).intValue();
        return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut);
    }

}
