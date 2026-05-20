package fr.robie.messageflow.model;

/**
 * Enumeration of supported message types that can be sent to players.
 */
public enum MessageType {
    /**
     * A boss bar message.
     */
    BOSS_BAR,

    /**
     * An action bar message displayed above the hotbar.
     */
    ACTION_BAR,
    /**
     * A standard chat message.
     */
    TCHAT,
    /**
     * A title message displayed in the center of the screen.
     */
    TITLE,
    /**
     * No specific message type.
     */
    NONE,
    /**
     * A chat message sent without the configured getPrefix.
     */
    WITHOUT_PREFIX,
    /**
     * A message broadcast to all online players.
     */
    BROADCAST,
    /**
     * A sound effect played to the player.
     */
    SOUND
}
