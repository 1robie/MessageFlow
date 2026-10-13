package fr.robie.messageflow.util;

/**
 * Enumeration of supported platform types for message formatting.
 * <p>
 * This class automatically detects whether Adventure API is available
 * at class loading time to determine the platform type.
 */
public enum PlatformType {
    /** Platform supports Adventure API (MiniMessage, Component). */
    COMPONENTS,
    /** Platform uses legacy Bukkit text formatting. */
    LEGACY;

    private static final PlatformType DETECTED;

    static {
        PlatformType type;
        try {
            Class.forName("net.kyori.adventure.text.Component");
            type = COMPONENTS;
        } catch (ClassNotFoundException e) {
            type = LEGACY;
        }
        DETECTED = type;
    }

    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets the detected platform type.
     *
     * @return the detected platform type
     */
    public static PlatformType get() {
        return DETECTED;
    }

    /**
     * Checks if the Adventure Component API is available.
     *
     * @return true if Adventure is available, false otherwise
     */
    public static boolean hasComponent() {
        return DETECTED == COMPONENTS;
    }

    /**
     * Checks if the server is Folia, where each player is ticked by its own region thread.
     *
     * @return true on Folia
     */
    public static boolean isFolia() {
        return FOLIA;
    }

}
