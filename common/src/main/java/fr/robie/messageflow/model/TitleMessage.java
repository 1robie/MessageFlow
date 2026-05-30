package fr.robie.messageflow.model;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.MessageTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A message adapter for title messages displayed in the center of the screen.
 * Contains a title, optional subtitle, and timing parameters.
 */
public final class TitleMessage extends MessageTypeAdapter {
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public TitleMessage(@NotNull String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
        this(title, subtitle, fadeIn, stay, fadeOut, false, false, false);
    }

    public TitleMessage(@NotNull String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut,
                        boolean broadcast, boolean sendToConsole, boolean excludeSenders) {
        super(broadcast, sendToConsole, excludeSenders);
        this.title = Preconditions.checkNotNull(title, "Title cannot be null");
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    @Override
    public @NotNull MessageType messageType() {
        return MessageType.TITLE;
    }

    public @NotNull String title() {
        return this.title;
    }

    public @Nullable String subtitle() {
        return this.subtitle;
    }

    public int fadeIn() {
        return this.fadeIn;
    }

    public int stay() {
        return this.stay;
    }

    public int fadeOut() {
        return this.fadeOut;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", this.title);
        map.put("subtitle", this.subtitle != null ? this.subtitle : "");
        map.put("fade-in", this.fadeIn);
        map.put("stay", this.stay);
        map.put("fade-out", this.fadeOut);
        this.serializeSettings(map);
        return map;
    }

    public static TitleMessage deserialize(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");
        String subtitle = (String) map.getOrDefault("subtitle", null);
        int fadeIn = ((Number) map.getOrDefault("fade-in", 10)).intValue();
        int stay = ((Number) map.getOrDefault("stay", 70)).intValue();
        int fadeOut = ((Number) map.getOrDefault("fade-out", 20)).intValue();

        boolean[] settings = parseSettings(map);
        return new TitleMessage(title, subtitle, fadeIn, stay, fadeOut, settings[0], settings[1], settings[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        TitleMessage that = (TitleMessage) o;
        return this.broadcast() == that.broadcast() &&
                this.sendToConsole() == that.sendToConsole() &&
                this.excludeSenders() == that.excludeSenders() &&
                this.fadeIn == that.fadeIn &&
                this.stay == that.stay &&
                this.fadeOut == that.fadeOut &&
                Objects.equals(this.title, that.title) &&
                Objects.equals(this.subtitle, that.subtitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.title, this.subtitle, this.fadeIn, this.stay, this.fadeOut, this.broadcast(), this.sendToConsole(), this.excludeSenders());
    }

    @Override
    public String toString() {
        return "TitleMessage[" +
                "title='" + this.title + '\'' +
                ", subtitle='" + this.subtitle + '\'' +
                ", fadeIn=" + this.fadeIn +
                ", stay=" + this.stay +
                ", fadeOut=" + this.fadeOut +
                ", broadcast=" + this.broadcast() +
                ", sendToConsole=" + this.sendToConsole() +
                ", excludeSenders=" + this.excludeSenders() +
                ']';
    }
}
