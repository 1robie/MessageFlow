package fr.robie.messageflow.util;

public enum PlatformType {
    COMPONENTS,
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

    public static PlatformType get() {
        return DETECTED;
    }

    public static boolean hasComponent() {
        return DETECTED == COMPONENTS;
    }

}
