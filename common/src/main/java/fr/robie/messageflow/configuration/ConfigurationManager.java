package fr.robie.messageflow.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the library's configuration lifecycle, including loading, synchronization,
 * and persistence of settings.
 * <p>
 * This class uses a central {@link Setting} enum to define all available configuration
 * options and their default values. It handles the synchronization between the in-memory
 * values and the physical YAML configuration file.
 *
 * @param <P> the type of the plugin using this manager
 */
public class ConfigurationManager<P extends Plugin> {
    private final P plugin;

    /**
     * Enumeration of all available configuration settings for the MessageFlow library.
     */
    public enum Setting {
        /**
         * Internal flag to bypass external configuration loading.
         * If set to true, the library will use default values and skip file operations.
         */
        BYPASS_EXTERNAL_CONFIG(null, Boolean.class, false, true),

        /**
         * The directory where MessageFlow configuration and language files are stored.
         * Internal setting, defaults to "messageflow".
         */
        CORE_DIRECTORY(null, String.class, "messageflow", true),

        /**
         * The name of the main configuration file for the library.
         * Internal setting, defaults to "messageflow.yml".
         */
        CORE_FILE_NAME(null, String.class, "messageflow.yml", true),

        /**
         * Whether missing language files should be automatically created.
         */
        SYNC_AUTO_CREATE("sync.auto-create", Boolean.class, true),

        /**
         * Whether missing keys in existing language files should be automatically added.
         */
        SYNC_AUTO_ADD_MISSING("sync.auto-add-missing", Boolean.class, true),

        /**
         * Whether obsolete keys in language files should be automatically removed.
         */
        SYNC_AUTO_REMOVE_OBSOLETE("sync.auto-remove-obsolete", Boolean.class, false),

        /**
         * Whether to create backups before removing obsolete keys.
         */
        BACKUP_ENABLED("backup.enabled", Boolean.class, true),

        /**
         * The directory where configuration backups are stored.
         */
        BACKUP_DIRECTORY("backup.directory", String.class, "messageflow/backup"),

        /**
         * The date format used for backup file names.
         */
        BACKUP_DATE_FORMAT("backup.date-format", String.class, "yyyy-MM-dd_HH-mm-ss"),

        /**
         * The prefix used for legacy log messages.
         */
        LEGACY_LOGGER_PREFIX("logger.legacy-prefix", String.class, "§8[%plugin-full-name%§8]", true),

        /**
         * The prefix used for Adventure/MiniMessage log messages.
         */
        ADVENTURE_LOGGER_PREFIX("logger.adventure-prefix", String.class, "<dark_gray>[</dark_gray>%plugin-name% %plugin-version%<dark_gray>]</dark_gray>", true),

        /**
         * The maximum number of entries in the message cache.
         */
        MESSAGE_CACHE_MAX_SIZE("cache.messages.maximum-size", Long.class, 512L),

        /**
         * Time in minutes after last access when a message cache entry expires.
         */
        MESSAGE_CACHE_EXPIRE_AFTER_ACCESS("cache.messages.expire-after-access-minutes", Long.class, 10L),

        /**
         * Time in minutes after creation when a message cache entry expires.
         */
        MESSAGE_CACHE_EXPIRE_AFTER_WRITE("cache.messages.expire-after-write-minutes", Long.class, -1L),

        /**
         * The initial capacity of the message cache.
         */
        MESSAGE_CACHE_INITIAL_CAPACITY("cache.messages.initial-capacity", Integer.class, -1),

        /**
         * The concurrency level for the message cache.
         */
        MESSAGE_CACHE_CONCURRENCY_LEVEL("cache.messages.concurrency-level", Integer.class, -1),

        /**
         * Whether to record statistics for the message cache.
         */
        MESSAGE_CACHE_RECORD_STATS("cache.messages.record-stats", Boolean.class, false),

        /**
         * Whether to use soft references for message cache values.
         */
        MESSAGE_CACHE_SOFT_VALUES("cache.messages.soft-values", Boolean.class, false),

        /**
         * The maximum number of entries in the global placeholder cache.
         */
        PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE("cache.placeholders.global.maximum-size", Long.class, 1000L),

        /**
         * Time in minutes after last access when a global placeholder cache entry expires.
         */
        PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_ACCESS("cache.placeholders.global.expire-after-access-minutes", Long.class, -1L),

        /**
         * Time in minutes after creation when a global placeholder cache entry expires.
         */
        PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_WRITE("cache.placeholders.global.expire-after-write-minutes", Long.class, -1L),

        /**
         * The initial capacity of the global placeholder cache.
         */
        PLACEHOLDER_GLOBAL_CACHE_INITIAL_CAPACITY("cache.placeholders.global.initial-capacity", Integer.class, -1),

        /**
         * The concurrency level for the global placeholder cache.
         */
        PLACEHOLDER_GLOBAL_CACHE_CONCURRENCY_LEVEL("cache.placeholders.global.concurrency-level", Integer.class, -1),

        /**
         * Whether to record statistics for the global placeholder cache.
         */
        PLACEHOLDER_GLOBAL_CACHE_RECORD_STATS("cache.placeholders.global.record-stats", Boolean.class, false),

        /**
         * Whether to use soft references for global placeholder cache values.
         */
        PLACEHOLDER_GLOBAL_CACHE_SOFT_VALUES("cache.placeholders.global.soft-values", Boolean.class, false),

        /**
         * The maximum number of entries in per-player placeholder caches.
         */
        PLACEHOLDER_PLAYER_CACHE_MAX_SIZE("cache.placeholders.player.maximum-size", Long.class, 10000L),

        /**
         * Time in minutes after last access when a player placeholder cache entry expires.
         */
        PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_ACCESS("cache.placeholders.player.expire-after-access-minutes", Long.class, -1L),

        /**
         * Time in minutes after creation when a player placeholder cache entry expires.
         */
        PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_WRITE("cache.placeholders.player.expire-after-write-minutes", Long.class, -1L),

        /**
         * The initial capacity of per-player placeholder caches.
         */
        PLACEHOLDER_PLAYER_CACHE_INITIAL_CAPACITY("cache.placeholders.player.initial-capacity", Integer.class, -1),

        /**
         * The concurrency level for per-player placeholder caches.
         */
        PLACEHOLDER_PLAYER_CACHE_CONCURRENCY_LEVEL("cache.placeholders.player.concurrency-level", Integer.class, -1),

        /**
         * Whether to record statistics for per-player placeholder caches.
         */
        PLACEHOLDER_PLAYER_CACHE_RECORD_STATS("cache.placeholders.player.record-stats", Boolean.class, false),

        /**
         * Whether to use soft references for player placeholder cache values.
         */
        PLACEHOLDER_PLAYER_CACHE_SOFT_VALUES("cache.placeholders.player.soft-values", Boolean.class, false),

        /**
         * Whether to log a message when the PlaceholderAPI hook is loaded.
         */
        HOOK_PLACEHOLDER_API_LOG_ON_LOAD("hooks.placeholderapi.log-on-load", Boolean.class, true),

        /**
         * The message logged when a hook is loaded.
         * Supports %hook% placeholder.
         */
        HOOK_LOAD_MESSAGE("hooks.load-message", String.class, "Hook %hook% loaded successfully!", true);

        private static final Set<Class<?>> ALLOWED_TYPES = Set.of(
                String.class, Integer.class, Boolean.class, Long.class, Double.class
        );

        private final @Nullable String path;
        private final Class<?> type;
        private Object defaultValue;
        private Object value;
        private final boolean allowNull;
        private final boolean internal;


        <T> Setting(@NotNull String path, @NotNull Class<T> type, @Nullable T defaultValue) {
            this(path, type, defaultValue, false);
        }

        <T> Setting(@Nullable String path, @NotNull Class<T> type, @Nullable T defaultValue, boolean internal) {
            this.path = path;
            this.type = type;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
            this.allowNull = defaultValue == null;
            this.internal = internal;
        }

        /**
         * Gets the default value for this setting.
         *
         * @param <T> the type of the value
         * @return the default value
         */
        @SuppressWarnings("unchecked")
        public <T> T getDefaultValue() {
            return (T) this.defaultValue;
        }

        /**
         * Sets the default value for this setting.
         *
         * @param <T>   the type of the value
         * @param value the new default value
         * @throws IllegalArgumentException if the value type is unsupported or incorrect
         */
        public <T> void setDefaultValue(@Nullable T value) {
            if (value == null) {
                if (!this.allowNull) {
                    throw new IllegalArgumentException("Null value not allowed for " + this.name());
                }
            } else {
                if (!ALLOWED_TYPES.contains(value.getClass())) {
                    throw new IllegalArgumentException(
                            "Unsupported type: " + value.getClass().getSimpleName() + " for " + this.name()
                    );
                }
                if (!this.type.isInstance(value)) {
                    throw new IllegalArgumentException(
                            "Expected " + this.type.getSimpleName() + " for " + this.name() + ", got " + value.getClass().getSimpleName()
                    );
                }
            }
            this.defaultValue = value;
        }

        /**
         * Resets the current value of this setting to its default.
         */
        public void resetToDefault() {
            this.value = this.defaultValue;
        }

        /**
         * Gets the current value of this setting.
         *
         * @param <T> the type of the value
         * @return the current value
         */
        @SuppressWarnings("unchecked")
        public <T> T getValue() {
            return (T) this.value;
        }

        /**
         * Sets the current value of this setting.
         *
         * @param <T>   the type of the value
         * @param value the new value
         * @throws IllegalArgumentException if the value type is incorrect or null is not allowed
         */
        public <T> void setValue(@Nullable T value) {
            if (value == null) {
                if (!this.allowNull) {
                    throw new IllegalArgumentException("Null value not allowed for " + this.name());
                }
                this.value = null;
            } else if (!this.type.isInstance(value)) {
                this.value = switch (value) {
                    case Integer i when this.type == Long.class -> i.longValue();
                    case Long l when this.type == Integer.class -> l.intValue();
                    case Number number when this.type == Double.class -> number.doubleValue();
                    default ->
                            throw new IllegalArgumentException("Expected " + this.type.getSimpleName() + " for " + this.name() + ", got " + value.getClass().getSimpleName());
                };
            } else {
                this.value = value;
            }
        }

        /**
         * Gets the data type of this setting.
         *
         * @return the type class
         */
        public Class<?> getType() {
            return this.type;
        }

        /**
         * Checks if this setting is internal and should not be serialized to the configuration file.
         *
         * @return true if internal, false otherwise
         */
        public boolean isInternal() {
            return this.internal;
        }
    }

    /**
     * Creates a new ConfigurationManager for the specified plugin.
     *
     * @param plugin the plugin instance
     */
    public ConfigurationManager(P plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the configuration from the YAML file and synchronizes settings.
     * <p>
     * If the file doesn't exist, it attempts to copy a bundled resource or creates
     * an empty configuration.
     */
    public void load() {
        if (Setting.BYPASS_EXTERNAL_CONFIG.getValue()) {
            return;
        }

        String relativePath = Setting.CORE_DIRECTORY.getValue() + File.separator + Setting.CORE_FILE_NAME.getValue();
        File messageFlowFile = new File(this.plugin.getDataFolder(), relativePath);
        if (!messageFlowFile.exists()) {
            if (!this.tryCopyBundledResource(relativePath)) {
                ensureParentExists(messageFlowFile);
                YamlConfiguration config = new YamlConfiguration();
                this.sync(config, messageFlowFile);
            }
        }

        if (messageFlowFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(messageFlowFile);
            this.sync(config, messageFlowFile);
        }
    }

    private void sync(YamlConfiguration config, File file) {
        boolean changed = false;

        if (Setting.SYNC_AUTO_ADD_MISSING.<Boolean>getValue()) {
            YamlConfiguration bundled = this.loadBundledYaml(Setting.CORE_DIRECTORY.getValue() + File.separator + Setting.CORE_FILE_NAME.getValue());
            for (Setting setting : Setting.values()) {
                if (setting.isInternal() || setting.path == null) {
                    continue;
                }
                if (!config.contains(setting.path)) {
                    Object defaultValue = setting.getDefaultValue();
                    Object value = bundled != null ? bundled.get(setting.path, defaultValue) : defaultValue;
                    config.set(setting.path, value);
                    changed = true;
                }
            }
        }

        if (Setting.SYNC_AUTO_REMOVE_OBSOLETE.<Boolean>getValue()) {
            Set<String> validPaths = Arrays.stream(Setting.values())
                    .filter(s -> !s.isInternal() && s.path != null)
                    .map(s -> s.path)
                    .collect(Collectors.toSet());
            for (String key : config.getKeys(true)) {
                if (config.isConfigurationSection(key)) {
                    continue;
                }
                if (!validPaths.contains(key)) {
                    config.set(key, null);
                    changed = true;
                }
            }
        }

        if (changed) {
            this.saveQuietly(config, file);
        }

        for (Setting setting : Setting.values()) {
            if (setting.isInternal() || setting.path == null) {
                continue;
            }
            Object value = config.get(setting.path, setting.getDefaultValue());
            setting.setValue(value);
        }
    }

    private void saveQuietly(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    private boolean tryCopyBundledResource(@NotNull String relativePath) {
        try {
            if (this.plugin.getResource(relativePath) == null) {
                return false;
            }
            this.plugin.saveResource(relativePath, false);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private @Nullable YamlConfiguration loadBundledYaml(@NotNull String relativePath) {
        try (InputStream is = this.plugin.getResource(relativePath)) {
            if (is == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException exception) {
            return null;
        }
    }

    private static void ensureParentExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

}