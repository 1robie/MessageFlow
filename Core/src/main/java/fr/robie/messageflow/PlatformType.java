package fr.robie.messageflow;

public enum PlatformType {
    PAPER,
    SPIGOT;

    private static PlatformType detectedType;

    public static PlatformType detect() {
        if (detectedType == null) {
            try {
                Class.forName("io.papermc.paper.text.PaperComponents");
                detectedType = PAPER;
            } catch (ClassNotFoundException e) {
                detectedType = SPIGOT;
            }
        }
        return detectedType;
    }

    public static PlatformType get() {
        return detect();
    }

    public static boolean isPaper() {
        return get() == PAPER;
    }

    public static boolean isSpigot() {
        return get() == SPIGOT;
    }

}
