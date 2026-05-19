package fr.robie.messageflow.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Settings for a specific message, such as allowed message types.
 */
public record MessageSettings(
        @NotNull Set<MessageType> allowedTypes,
        @NotNull Set<MessageType> blockedTypes
) {
    /**
     * Default settings that allow all message types.
     */
    public static final MessageSettings DEFAULT = new MessageSettings(Collections.emptySet(), Collections.emptySet());

    /**
     * Creates a new MessageSettings that only allows the specified message types.
     *
     * @param types the allowed message types
     * @return a new MessageSettings instance with a whitelist
     */
    public static @NotNull MessageSettings whitelist(@NotNull MessageType... types) {
        return new MessageSettings(Set.of(types), Collections.emptySet());
    }

    /**
     * Creates a new MessageSettings that blocks the specified message types.
     *
     * @param types the blocked message types
     * @return a new MessageSettings instance with a blacklist
     */
    public static @NotNull MessageSettings blacklist(@NotNull MessageType... types) {
        return new MessageSettings(Collections.emptySet(), Set.of(types));
    }

    /**
     * Checks if a message type is allowed for this message.
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
