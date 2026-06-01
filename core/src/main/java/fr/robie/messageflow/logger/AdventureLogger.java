package fr.robie.messageflow.logger;

import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Logger implementation that uses Adventure (MiniMessage) for formatting.
 * <p>
 * This logger is suitable for modern Minecraft versions (1.16+) that support
 * rich text components and MiniMessage formatting.
 */
public final class AdventureLogger extends Logger {

    /**
     * Constructs a new AdventureLogger.
     *
     * @param prefix           the log getPrefix
     * @param messageFormatter the Adventure message formatter to use
     */
    public AdventureLogger(@NotNull String prefix, @NotNull AdventureMessageFormatter<? extends Plugin> messageFormatter) {
        super(prefix, messageFormatter);
    }

    @Override
    @NotNull
    protected String getErrorColor() {
        return "<red>";
    }

    @Override
    @NotNull
    protected String prefixe(@NotNull LogType type) {
        String colorForLogType = type.getAdventureColorCode();
        String typeName = (this.showTypeNames && type.isShowTypeName()) ? colorForLogType + "[" + type.name() + "] " : "";
        return typeName + "<reset>" + this.prefix + " " + (this.colorWholeMessage || type.isColorWholeMessage() ? colorForLogType : "");
    }
}
