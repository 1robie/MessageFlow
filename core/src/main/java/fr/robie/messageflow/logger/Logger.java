package fr.robie.messageflow.logger;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.Message;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for logging within the MessageFlow framework.
 * <p>
 * Provides both static access for global logging and instance-based logging.
 * Implementation classes handle the actual message formatting and delivery.
 */
@SuppressWarnings("unused")
public abstract class Logger {
    /**
     * The global logger instance.
     */
    private static Logger logger;

    /**
     * The getPrefix used in log messages.
     */
    protected final @NotNull String prefix;

    /**
     * The formatter used to process log messages.
     */
    protected final @NotNull MessageFormatter<?, ?> messageFormatter;

    /**
     * Whether debug logging is enabled.
     */
    protected boolean debugEnabled = false;

    /**
     * Whether to apply color formatting to the entire log message or just the log level getPrefix.
     */
    protected boolean colorWholeMessage = false;

    /**
     * Whether to show the log type name (e.g., [INFO]) in the prefix.
     */
    protected boolean showTypeNames = true;

    /**
     * Constructs a new Logger.
     *
     * @param prefix           the log getPrefix
     * @param messageFormatter the formatter to use
     * @throws IllegalStateException if a logger instance already exists (to prevent conflicts when not relocated)
     */
    protected Logger(@NotNull String prefix, @NotNull MessageFormatter<?, ?> messageFormatter) {
        Preconditions.checkNotNull(prefix, "Prefix cannot be null");
        Preconditions.checkNotNull(messageFormatter, "MessageFormatter cannot be null");
        if (logger != null) {
            throw new IllegalStateException("Logger instance already exists for package '" + this.getClass().getPackage().getName() +
                    "'. If you are using MessageFlow in multiple plugins, you MUST relocate (shade/rename) the library " +
                    "to avoid conflicts between different plugins sharing the same static state.");
        }
        this.prefix = prefix;
        this.messageFormatter = messageFormatter;
        logger = this;
    }

    /**
     * Logs an info message using the global logger.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public static void info(@NotNull String message, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.INFO, message, placeholders);
        }
    }

    /**
     * Logs an info message using the global logger.
     *
     * @param message the message to log
     */
    public static void info(@NotNull String message) {
        info(message, Placeholder.empty());
    }

    /**
     * Logs an info message using the global logger.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public static void info(@NotNull Message message, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.INFO, message, placeholders);
        }
    }

    /**
     * Logs an info message using the global logger.
     *
     * @param message the Message object to log
     */
    public static void info(@NotNull Message message) {
        info(message, Placeholder.empty());
    }

    /**
     * Logs a warning message using the global logger.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public static void warn(@NotNull String message, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.WARNING, message, placeholders);
        }
    }

    /**
     * Logs a warning message using the global logger.
     *
     * @param message the message to log
     */
    public static void warn(@NotNull String message) {
        warn(message, Placeholder.empty());
    }

    /**
     * Logs a warning message using the global logger.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public static void warn(@NotNull Message message, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.WARNING, message, placeholders);
        }
    }

    /**
     * Logs a warning message using the global logger.
     *
     * @param message the Message object to log
     */
    public static void warn(@NotNull Message message) {
        warn(message, Placeholder.empty());
    }

    /**
     * Logs an error message using the global logger.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public static void error(@NotNull String message, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.ERROR, message, placeholders);
        }
    }

    /**
     * Logs an error message using the global logger.
     *
     * @param message the message to log
     */
    public static void error(@NotNull String message) {
        error(message, Placeholder.empty());
    }

    /**
     * Logs an error message using the global logger.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public static void error(@NotNull Message message, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.ERROR, message, placeholders);
        }
    }

    /**
     * Logs an error message using the global logger.
     *
     * @param message the Message object to log
     */
    public static void error(@NotNull Message message) {
        error(message, Placeholder.empty());
    }

    /**
     * Logs a debug message using the global logger if debug mode is enabled.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public static void debug(@NotNull String message, @NotNull Placeholder placeholders) {
        if (logger != null && logger.debugEnabled) {
            logger.log(LogType.DEBUG, message, placeholders);
        }
    }

    /**
     * Logs a debug message using the global logger if debug mode is enabled.
     *
     * @param message the message to log
     */
    public static void debug(@NotNull String message) {
        debug(message, Placeholder.empty());
    }

    /**
     * Logs a debug message using the global logger if debug mode is enabled.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public static void debug(@NotNull Message message, @NotNull Placeholder placeholders) {
        if (logger != null && logger.debugEnabled) {
            logger.log(LogType.DEBUG, message, placeholders);
        }
    }

    /**
     * Logs a debug message using the global logger if debug mode is enabled.
     *
     * @param message the Message object to log
     */
    public static void debug(@NotNull Message message) {
        debug(message, Placeholder.empty());
    }

    /**
     * Logs a debug message with a stacktrace using the global logger if debug mode is enabled.
     *
     * @param message      the message to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public static void debug(@NotNull String message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (logger != null && logger.debugEnabled) {
            logger.log(LogType.DEBUG, throwable, message, placeholders);
        }
    }

    /**
     * Logs a debug message with a stacktrace using the global logger if debug mode is enabled.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public static void debug(@NotNull String message, @NotNull Throwable throwable) {
        debug(message, throwable, Placeholder.empty());
    }

    /**
     * Logs a debug message with a stacktrace using the global logger if debug mode is enabled.
     *
     * @param message      the Message object to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public static void debug(@NotNull Message message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (logger != null && logger.debugEnabled) {
            logger.log(LogType.DEBUG, throwable, message, placeholders);
        }
    }

    /**
     * Logs a debug message with a stacktrace using the global logger if debug mode is enabled.
     *
     * @param message   the Message object to log
     * @param throwable the exception to log
     */
    public static void debug(@NotNull Message message, @NotNull Throwable throwable) {
        debug(message, throwable, Placeholder.empty());
    }

    /**
     * Logs a warning message with a stacktrace using the global logger.
     *
     * @param message      the message to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public static void warn(@NotNull String message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.WARNING, throwable, message, placeholders);
        }
    }

    /**
     * Logs a warning message with a stacktrace using the global logger.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public static void warn(@NotNull String message, @NotNull Throwable throwable) {
        warn(message, throwable, Placeholder.empty());
    }

    /**
     * Logs a warning message with a stacktrace using the global logger.
     *
     * @param message      the Message object to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public static void warn(@NotNull Message message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.WARNING, throwable, message, placeholders);
        }
    }

    /**
     * Logs a warning message with a stacktrace using the global logger.
     *
     * @param message   the Message object to log
     * @param throwable the exception to log
     */
    public static void warn(@NotNull Message message, @NotNull Throwable throwable) {
        warn(message, throwable, Placeholder.empty());
    }

    /**
     * Logs an error message with a stacktrace using the global logger.
     *
     * @param message      the message to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.ERROR, throwable, message, placeholders);
        }
    }

    /**
     * Logs an error message with a stacktrace using the global logger.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable) {
        error(message, throwable, Placeholder.empty());
    }

    /**
     * Logs an error message with a stacktrace using the global logger.
     *
     * @param message      the Message object to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public static void error(@NotNull Message message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (logger != null) {
            logger.log(LogType.ERROR, throwable, message, placeholders);
        }
    }

    /**
     * Logs an error message with a stacktrace using the global logger.
     *
     * @param message   the Message object to log
     * @param throwable the exception to log
     */
    public static void error(@NotNull Message message, @NotNull Throwable throwable) {
        error(message, throwable, Placeholder.empty());
    }

    /**
     * Sets whether debug mode is enabled for the global logger.
     *
     * @param enabled true to enable, false to disable
     */
    public static void setDebug(boolean enabled) {
        if (logger != null) {
            logger.setDebugEnabled(enabled);
        }
    }

    /**
     * Toggles the debug mode for the global logger.
     */
    public static void toggleDebug() {
        if (logger != null) {
            logger.setDebugEnabled(!logger.isDebugEnabled());
        }
    }

    /**
     * Checks if debug mode is enabled for the global logger.
     *
     * @return true if debug mode is enabled
     */
    public static boolean isDebug() {
        return logger != null && logger.isDebugEnabled();
    }

    /**
     * Gets the prefix for a specific log type from the global logger.
     *
     * @param type the log type
     * @return the formatted prefix
     */
    @NotNull
    public static String getPrefix(@NotNull LogType type) {
        if (logger != null) {
            return logger.prefixe(type);
        }
        return "[" + type.name() + "] ";
    }

    /**
     * Logs an info message using this logger instance.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public void logInfo(@NotNull String message, @NotNull Placeholder placeholders) {
        this.log(LogType.INFO, message, placeholders);
    }

    /**
     * Logs an info message using this logger instance.
     *
     * @param message the message to log
     */
    public void logInfo(@NotNull String message) {
        this.logInfo(message, Placeholder.empty());
    }

    /**
     * Logs an info message using this logger instance.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public void logInfo(@NotNull Message message, @NotNull Placeholder placeholders) {
        this.log(LogType.INFO, message, placeholders);
    }

    /**
     * Logs an info message using this logger instance.
     *
     * @param message the Message object to log
     */
    public void logInfo(@NotNull Message message) {
        this.logInfo(message, Placeholder.empty());
    }

    /**
     * Logs a warning message using this logger instance.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public void logWarn(@NotNull String message, @NotNull Placeholder placeholders) {
        this.log(LogType.WARNING, message, placeholders);
    }

    /**
     * Logs a warning message using this logger instance.
     *
     * @param message the message to log
     */
    public void logWarn(@NotNull String message) {
        this.logWarn(message, Placeholder.empty());
    }

    /**
     * Logs a warning message using this logger instance.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public void logWarn(@NotNull Message message, @NotNull Placeholder placeholders) {
        this.log(LogType.WARNING, message, placeholders);
    }

    /**
     * Logs a warning message using this logger instance.
     *
     * @param message the Message object to log
     */
    public void logWarn(@NotNull Message message) {
        this.logWarn(message, Placeholder.empty());
    }

    /**
     * Logs an error message using this logger instance.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public void logError(@NotNull String message, @NotNull Placeholder placeholders) {
        this.log(LogType.ERROR, message, placeholders);
    }

    /**
     * Logs an error message using this logger instance.
     *
     * @param message the message to log
     */
    public void logError(@NotNull String message) {
        this.logError(message, Placeholder.empty());
    }

    /**
     * Logs an error message using this logger instance.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public void logError(@NotNull Message message, @NotNull Placeholder placeholders) {
        this.log(LogType.ERROR, message, placeholders);
    }

    /**
     * Logs an error message using this logger instance.
     *
     * @param message the Message object to log
     */
    public void logError(@NotNull Message message) {
        this.logError(message, Placeholder.empty());
    }

    /**
     * Logs a debug message using this logger instance if debug mode is enabled.
     *
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    public void logDebug(@NotNull String message, @NotNull Placeholder placeholders) {
        if (this.debugEnabled) {
            this.log(LogType.DEBUG, message, placeholders);
        }
    }

    /**
     * Logs a debug message using this logger instance if debug mode is enabled.
     *
     * @param message the message to log
     */
    public void logDebug(@NotNull String message) {
        this.logDebug(message, Placeholder.empty());
    }

    /**
     * Logs a debug message using this logger instance if debug mode is enabled.
     *
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    public void logDebug(@NotNull Message message, @NotNull Placeholder placeholders) {
        if (this.debugEnabled) {
            this.log(LogType.DEBUG, message, placeholders);
        }
    }

    /**
     * Logs a debug message using this logger instance if debug mode is enabled.
     *
     * @param message the Message object to log
     */
    public void logDebug(@NotNull Message message) {
        this.logDebug(message, Placeholder.empty());
    }

    /**
     * Logs a debug message with a stacktrace using this logger instance if debug mode is enabled.
     *
     * @param message      the message to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public void logDebug(@NotNull String message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (this.debugEnabled) {
            this.log(LogType.DEBUG, throwable, message, placeholders);
        }
    }

    /**
     * Logs a debug message with a stacktrace using this logger instance if debug mode is enabled.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public void logDebug(@NotNull String message, @NotNull Throwable throwable) {
        this.logDebug(message, throwable, Placeholder.empty());
    }

    /**
     * Logs a debug message with a stacktrace using this logger instance if debug mode is enabled.
     *
     * @param message      the Message object to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public void logDebug(@NotNull Message message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        if (this.debugEnabled) {
            this.log(LogType.DEBUG, throwable, message, placeholders);
        }
    }

    /**
     * Logs a debug message with a stacktrace using this logger instance if debug mode is enabled.
     *
     * @param message   the Message object to log
     * @param throwable the exception to log
     */
    public void logDebug(@NotNull Message message, @NotNull Throwable throwable) {
        this.logDebug(message, throwable, Placeholder.empty());
    }

    /**
     * Logs a warning message with a stacktrace using this logger instance.
     *
     * @param message      the message to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public void logWarn(@NotNull String message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        this.log(LogType.WARNING, throwable, message, placeholders);
    }

    /**
     * Logs a warning message with a stacktrace using this logger instance.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public void logWarn(@NotNull String message, @NotNull Throwable throwable) {
        this.logWarn(message, throwable, Placeholder.empty());
    }

    /**
     * Logs a warning message with a stacktrace using this logger instance.
     *
     * @param message      the Message object to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public void logWarn(@NotNull Message message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        this.log(LogType.WARNING, throwable, message, placeholders);
    }

    /**
     * Logs a warning message with a stacktrace using this logger instance.
     *
     * @param message   the Message object to log
     * @param throwable the exception to log
     */
    public void logWarn(@NotNull Message message, @NotNull Throwable throwable) {
        this.logWarn(message, throwable, Placeholder.empty());
    }

    /**
     * Logs an error message with a stacktrace using this logger instance.
     *
     * @param message      the message to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public void logError(@NotNull String message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        this.log(LogType.ERROR, throwable, message, placeholders);
    }

    /**
     * Logs an error message with a stacktrace using this logger instance.
     *
     * @param message   the message to log
     * @param throwable the exception to log
     */
    public void logError(@NotNull String message, @NotNull Throwable throwable) {
        this.logError(message, throwable, Placeholder.empty());
    }

    /**
     * Logs an error message with a stacktrace using this logger instance.
     *
     * @param message      the Message object to log
     * @param throwable    the exception to log
     * @param placeholders formatting placeholders
     */
    public void logError(@NotNull Message message, @NotNull Throwable throwable, @NotNull Placeholder placeholders) {
        this.log(LogType.ERROR, throwable, message, placeholders);
    }

    /**
     * Logs an error message with a stacktrace using this logger instance.
     *
     * @param message   the Message object to log
     * @param throwable the exception to log
     */
    public void logError(@NotNull Message message, @NotNull Throwable throwable) {
        this.logError(message, throwable, Placeholder.empty());
    }

    /**
     * Implementation-specific logging logic.
     *
     * @param type         the type of log
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    protected abstract void log(@NotNull LogType type, @NotNull String message, @NotNull Placeholder placeholders);

    /**
     * Implementation-specific logging logic with a stacktrace.
     *
     * @param type         the type of log
     * @param throwable    the exception to log
     * @param message      the message to log
     * @param placeholders formatting placeholders
     */
    protected abstract void log(@NotNull LogType type, @NotNull Throwable throwable, @NotNull String message, @NotNull Placeholder placeholders);

    /**
     * Implementation-specific logging logic for Message objects.
     *
     * @param type         the type of log
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    protected abstract void log(@NotNull LogType type, @NotNull Message message, @NotNull Placeholder placeholders);

    /**
     * Implementation-specific logging logic for Message objects with a stacktrace.
     *
     * @param type         the type of log
     * @param throwable    the exception to log
     * @param message      the Message object to log
     * @param placeholders formatting placeholders
     */
    protected abstract void log(@NotNull LogType type, @NotNull Throwable throwable, @NotNull Message message, @NotNull Placeholder placeholders);

    /**
     * Gets the formatted prefix for a log type.
     *
     * @param type the log type
     * @return the prefix string
     */
    @NotNull
    protected abstract String prefixe(@NotNull LogType type);

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return this.debugEnabled;
    }

    /**
     * Enables or disables debug mode.
     *
     * @param debugEnabled true to enable, false to disable
     */
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    /**
     * Checks if the entire log message is colored.
     *
     * @return true if the whole message is colored
     */
    public boolean isColorWholeMessage() {
        return this.colorWholeMessage;
    }

    /**
     * Sets whether the entire log message should be colored.
     *
     * @param colorWholeMessage true to color the whole message
     */
    public void setColorWholeMessage(boolean colorWholeMessage) {
        this.colorWholeMessage = colorWholeMessage;
    }

    /**
     * Checks if log type names (e.g., [INFO]) should be shown.
     *
     * @return true if type names should be shown
     */
    public boolean isShowTypeNames() {
        return this.showTypeNames;
    }

    /**
     * Sets whether log type names (e.g., [INFO]) should be shown.
     *
     * @param showTypeNames true to show type names
     */
    public void setShowTypeNames(boolean showTypeNames) {
        this.showTypeNames = showTypeNames;
    }

    /**
     * Sets whether the entire log message should be colored for the global logger.
     *
     * @param enabled true to color the whole message
     */
    public static void setColorWhole(boolean enabled) {
        if (logger != null) {
            logger.setColorWholeMessage(enabled);
        }
    }

    /**
     * Checks if the entire log message is colored for the global logger.
     *
     * @return true if the whole message is colored
     */
    public static boolean isColorWhole() {
        return logger != null && logger.isColorWholeMessage();
    }

    /**
     * Sets whether log type names (e.g., [INFO]) should be shown for the global logger.
     *
     * @param enabled true to show type names
     */
    public static void setShowTypeNamesGlobal(boolean enabled) {
        if (logger != null) {
            logger.setShowTypeNames(enabled);
        }
    }

    /**
     * Checks if log type names are shown for the global logger.
     *
     * @return true if type names are shown
     */
    public static boolean isShowTypeNamesGlobal() {
        return logger != null && logger.isShowTypeNames();
    }

    /**
     * Gets the global logger instance.
     *
     * @return the logger instance, or null if not initialized
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Enumeration of log types.
     */
    public enum LogType {
        /**
         * Informational messages.
         */
        INFO("<blue>", "&9", false),
        /**
         * Warning messages.
         */
        WARNING("<gold>", "&6", false),
        /**
         * Error messages.
         */
        ERROR("<red>", "&c", true),
        /**
         * Debugging messages.
         */
        DEBUG("<light_purple>", "&d", false);
        private String adventureColorCode;
        private String legacyColorCode;

        private final boolean defaultColorWholeMessage;

        private boolean colorWholeMessage;

        private boolean showTypeName = true;

        LogType(@NotNull String adventureColorCode, @NotNull String legacyColorCode, boolean defaultColorWholeMessage) {
            this.adventureColorCode = adventureColorCode;
            this.legacyColorCode = legacyColorCode;
            this.defaultColorWholeMessage = defaultColorWholeMessage;
            this.colorWholeMessage = defaultColorWholeMessage;
        }

        /**
         * Checks if the entire log message should be colored for this log type.
         *
         * @return true if the whole message should be colored
         */
        public boolean isColorWholeMessage() {
            return this.colorWholeMessage;
        }

        /**
         * Sets whether the entire log message should be colored for this log type.
         *
         * @param colorWholeMessage true to color the whole message
         */
        public void setColorWholeMessage(boolean colorWholeMessage) {
            this.colorWholeMessage = colorWholeMessage;
        }

        /**
         * Toggles the colored whole message state for this log type.
         */
        public void toggleColorWholeMessage() {
            this.colorWholeMessage = !this.colorWholeMessage;
        }

        /**
         * Resets the colored whole message state to its default value for this log type.
         */
        public void resetColorWholeMessage() {
            this.colorWholeMessage = this.defaultColorWholeMessage;
        }

        /**
         * Checks if the type name (e.g., [INFO]) should be shown for this log type.
         *
         * @return true if the type name should be shown
         */
        public boolean isShowTypeName() {
            return this.showTypeName;
        }

        /**
         * Sets whether the type name should be shown for this log type.
         *
         * @param showTypeName true to show the type name
         */
        public void setShowTypeName(boolean showTypeName) {
            this.showTypeName = showTypeName;
        }

        /**
         * Toggles the visibility of the type name for this log type.
         */
        public void toggleShowTypeName() {
            this.showTypeName = !this.showTypeName;
        }

        @NotNull
        public String getLegacyColorCode() {
            return this.legacyColorCode;
        }

        @NotNull
        public String getAdventureColorCode() {
            return this.adventureColorCode;
        }

        public void setLegacyColorCode(@NotNull String legacyColorCode) {
            Preconditions.checkNotNull(legacyColorCode, "Legacy color code cannot be null");
            this.legacyColorCode = legacyColorCode;
        }

        public void setAdventureColorCode(@NotNull String adventureColorCode) {
            Preconditions.checkNotNull(adventureColorCode, "Adventure color code cannot be null");
            this.adventureColorCode = adventureColorCode;
        }
    }
}
