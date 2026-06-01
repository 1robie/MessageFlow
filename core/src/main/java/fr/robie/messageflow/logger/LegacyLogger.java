package fr.robie.messageflow.logger;

import fr.robie.messageflow.formatter.LegacyMessageFormatter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Logger implementation that uses Legacy (ChatColor) for formatting.
 * <p>
 * This logger is suitable for older Minecraft versions or environments
 * where Adventure/MiniMessage is not available or preferred.
 */
public final class LegacyLogger extends Logger {

    /**
     * Constructs a new LegacyLogger.
     *
     * @param prefix           the log getPrefix
     * @param messageFormatter the Legacy message formatter to use
     */
    public LegacyLogger(@NotNull String prefix, @NotNull LegacyMessageFormatter<? extends Plugin> messageFormatter) {
        super(prefix, messageFormatter);
    }

    @Override
    @NotNull
    protected String getErrorColor() {
        return "&c";
    }

    @Override
    @NotNull
    protected String prefixe(@NotNull LogType type) {
        String colorForLogType = type.getLegacyColorCode();
        String typeName = (this.showTypeNames && type.isShowTypeName()) ? colorForLogType + "[" + type.name() + "] " : "";
        return typeName + "&r" + this.prefix + " " + (this.colorWholeMessage || type.isColorWholeMessage() ? colorForLogType : "");
    }
}
