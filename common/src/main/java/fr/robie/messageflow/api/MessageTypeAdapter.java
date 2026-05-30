package fr.robie.messageflow.api;

import fr.robie.messageflow.model.MessageType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface representing a message adapter for a specific message type.
 */
public abstract class MessageTypeAdapter {
    private boolean broadcast;
    private boolean sendToConsole;
    private boolean excludeSenders;

    protected MessageTypeAdapter() {
        this(false, false, false);
    }

    protected MessageTypeAdapter(boolean broadcast, boolean sendToConsole, boolean excludeSenders) {
        this.broadcast = broadcast;
        this.sendToConsole = sendToConsole;
        this.excludeSenders = excludeSenders;
    }

    /**
     * Retrieves the specific type of this message handled by the adapter. This type determines
     * how the message will be serialized or presented to the system or end-user.
     *
     * @return the type of the message represented as {@link MessageType}
     */
    @NotNull
    public abstract MessageType messageType();

    /**
     * Serializes the message into a map of values for storage.
     *
     * @return the serialized message map
     */
    @NotNull
    public abstract Map<String, Object> serialize();

    /**
     * @return true if this component should be broadcast to all online players
     */
    public boolean broadcast() {
        return this.broadcast;
    }

    /**
     * @return true if this component should be sent to the console
     */
    public boolean sendToConsole() {
        return this.sendToConsole;
    }

    /**
     * @return true if the initial recipients should be excluded from the broadcast
     */
    public boolean excludeSenders() {
        return this.excludeSenders;
    }

    /**
     * Sets whether this component should be broadcast to all online players.
     *
     * @param broadcast true to enable broadcasting, false to disable
     */
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    /**
     * Sets whether this component should be sent to the console.
     *
     * @param sendToConsole true to enable console sending, false to disable
     */
    public void setSendToConsole(boolean sendToConsole) {
        this.sendToConsole = sendToConsole;
    }

    /**
     * Sets whether the initial recipients should be excluded from the broadcast.
     *
     * @param excludeSenders true to enable sender exclusion, false to disable
     */
    public void setExcludeSenders(boolean excludeSenders) {
        this.excludeSenders = excludeSenders;
    }

    /**
     * Helper to serialize settings into a map.
     *
     * @param map the map to add settings to
     */
    protected void serializeSettings(@NotNull Map<String, Object> map) {
        if (this.broadcast) {
            map.put("broadcast", true);
        }
        if (this.sendToConsole) {
            map.put("send-to-console", true);
        }
        if (this.excludeSenders) {
            map.put("exclude-senders", true);
        }
    }

    /**
     * Helper to parse settings from a map.
     *
     * @param map the map to parse from
     * @return an array of 3 booleans: [broadcast, sendToConsole, excludeSenders]
     */
    protected static boolean[] parseSettings(@NotNull Map<String, Object> map) {
        return new boolean[]{
                (boolean) map.getOrDefault("broadcast", false),
                (boolean) map.getOrDefault("send-to-console", false),
                (boolean) map.getOrDefault("exclude-senders", false)
        };
    }
}
