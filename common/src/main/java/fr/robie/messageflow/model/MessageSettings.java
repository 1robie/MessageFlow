package fr.robie.messageflow.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Settings for a specific message, such as allowed message types.
 */
public record MessageSettings(
        @NotNull Set<MessageType> allowedTypes,
        @NotNull Set<MessageType> blockedTypes,
        boolean broadcast,
        boolean sendToConsole,
        boolean excludeSenders
) {
    /**
     * Default settings that allow all message types.
     */
    public static final MessageSettings DEFAULT = new MessageSettings(
            Collections.emptySet(),
            Collections.emptySet(),
            false,
            false,
            false
    );

    /**
     * Creates a new MessageSettings that only allows the specified message types.
     *
     * @param types the allowed message types
     * @return a new MessageSettings instance with a whitelist
     */
    public static @NotNull MessageSettings whitelist(@NotNull MessageType... types) {
        return new MessageSettings(Set.of(types), Collections.emptySet(), false, false, false);
    }

    /**
     * Creates a new MessageSettings that blocks the specified message types.
     *
     * @param types the blocked message types
     * @return a new MessageSettings instance with a blacklist
     */
    public static @NotNull MessageSettings blacklist(@NotNull MessageType... types) {
        return new MessageSettings(Collections.emptySet(), Set.of(types), false, false, false);
    }

    /**
     * Creates a new MessageSettings with broadcast enabled.
     *
     * @return a new MessageSettings instance
     */
    public @NotNull MessageSettings withBroadcast(boolean broadcast) {
        return new MessageSettings(this.allowedTypes, this.blockedTypes, broadcast, this.sendToConsole, this.excludeSenders);
    }

    /**
     * Creates a new MessageSettings with console sending enabled.
     *
     * @return a new MessageSettings instance
     */
    public @NotNull MessageSettings withSendToConsole(boolean sendToConsole) {
        return new MessageSettings(this.allowedTypes, this.blockedTypes, this.broadcast, sendToConsole, this.excludeSenders);
    }

    /**
     * Creates a new MessageSettings with sender exclusion enabled.
     *
     * @return a new MessageSettings instance
     */
    public @NotNull MessageSettings withExcludeSenders(boolean excludeSenders) {
        return new MessageSettings(this.allowedTypes, this.blockedTypes, this.broadcast, this.sendToConsole, excludeSenders);
    }

    /**
     * Checks whether a message type is allowed based on these settings.
     *
     * @param type the message type to check
     * @return true if the type is allowed, false otherwise
     */
    public boolean isTypeAllowed(@NotNull MessageType type) {
        if (!this.allowedTypes.isEmpty() && !this.allowedTypes.contains(type)) {
            return false;
        }
        return !this.blockedTypes.contains(type);
    }
}
