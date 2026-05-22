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

public class ConfigurationManager<P extends Plugin> {
    private final P plugin;

    public enum Setting {
        BYPASS_EXTERNAL_CONFIG(null, Boolean.class, false, true),

        CORE_DIRECTORY(null, String.class, "messageflow", true),
        CORE_FILE_NAME(null, String.class, "messageflow.yml", true),

        SYNC_AUTO_CREATE("sync.auto-create", Boolean.class, true),
        SYNC_AUTO_ADD_MISSING("sync.auto-add-missing", Boolean.class, true),
        SYNC_AUTO_REMOVE_OBSOLETE("sync.auto-remove-obsolete", Boolean.class, false),

        BACKUP_ENABLED("backup.enabled", Boolean.class, true),
        BACKUP_DIRECTORY("backup.directory", String.class, "messageflow/backup"),
        BACKUP_DATE_FORMAT("backup.date-format", String.class, "yyyy-MM-dd_HH-mm-ss"),

        LEGACY_LOGGER_PREFIX("logger.legacy-prefix", String.class, "§8[%plugin-full-name%§8]", true),
        ADVENTURE_LOGGER_PREFIX("logger.adventure-prefix", String.class, "<dark_gray>[</dark_gray>%plugin-name% %plugin-version%<dark_gray>]</dark_gray>", true),

        MESSAGE_CACHE_MAX_SIZE("cache.messages.maximum-size", Long.class, 512L),
        MESSAGE_CACHE_EXPIRE_AFTER_ACCESS("cache.messages.expire-after-access-minutes", Long.class, 10L),
        MESSAGE_CACHE_EXPIRE_AFTER_WRITE("cache.messages.expire-after-write-minutes", Long.class, -1L),
        MESSAGE_CACHE_INITIAL_CAPACITY("cache.messages.initial-capacity", Integer.class, -1),
        MESSAGE_CACHE_CONCURRENCY_LEVEL("cache.messages.concurrency-level", Integer.class, -1),
        MESSAGE_CACHE_RECORD_STATS("cache.messages.record-stats", Boolean.class, false),
        MESSAGE_CACHE_SOFT_VALUES("cache.messages.soft-values", Boolean.class, false),

        PLACEHOLDER_GLOBAL_CACHE_MAX_SIZE("cache.placeholders.global.maximum-size", Long.class, 1000L),
        PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_ACCESS("cache.placeholders.global.expire-after-access-minutes", Long.class, -1L),
        PLACEHOLDER_GLOBAL_CACHE_EXPIRE_AFTER_WRITE("cache.placeholders.global.expire-after-write-minutes", Long.class, -1L),
        PLACEHOLDER_GLOBAL_CACHE_INITIAL_CAPACITY("cache.placeholders.global.initial-capacity", Integer.class, -1),
        PLACEHOLDER_GLOBAL_CACHE_CONCURRENCY_LEVEL("cache.placeholders.global.concurrency-level", Integer.class, -1),
        PLACEHOLDER_GLOBAL_CACHE_RECORD_STATS("cache.placeholders.global.record-stats", Boolean.class, false),
        PLACEHOLDER_GLOBAL_CACHE_SOFT_VALUES("cache.placeholders.global.soft-values", Boolean.class, false),

        PLACEHOLDER_PLAYER_CACHE_MAX_SIZE("cache.placeholders.player.maximum-size", Long.class, 10000L),
        PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_ACCESS("cache.placeholders.player.expire-after-access-minutes", Long.class, -1L),
        PLACEHOLDER_PLAYER_CACHE_EXPIRE_AFTER_WRITE("cache.placeholders.player.expire-after-write-minutes", Long.class, -1L),
        PLACEHOLDER_PLAYER_CACHE_INITIAL_CAPACITY("cache.placeholders.player.initial-capacity", Integer.class, -1),
        PLACEHOLDER_PLAYER_CACHE_CONCURRENCY_LEVEL("cache.placeholders.player.concurrency-level", Integer.class, -1),
        PLACEHOLDER_PLAYER_CACHE_RECORD_STATS("cache.placeholders.player.record-stats", Boolean.class, false),
        PLACEHOLDER_PLAYER_CACHE_SOFT_VALUES("cache.placeholders.player.soft-values", Boolean.class, false);

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

        @SuppressWarnings("unchecked")
        public <T> T getDefaultValue() {
            return (T) this.defaultValue;
        }

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

        public void resetToDefault() {
            this.value = this.defaultValue;
        }

        @SuppressWarnings("unchecked")
        public <T> T getValue() {
            return (T) this.value;
        }

        public <T> void setValue(@Nullable T value) {
            if (value == null) {
                if (!this.allowNull) {
                    throw new IllegalArgumentException("Null value not allowed for " + this.name());
                }
            } else if (!this.type.isInstance(value)) {
                throw new IllegalArgumentException(
                        "Expected " + this.type.getSimpleName() + " for " + this.name() + ", got " + value.getClass().getSimpleName()
                );
            }
            this.value = value;
        }

        public Class<?> getType() {
            return this.type;
        }

        public boolean isInternal() {
            return this.internal;
        }
    }

    public ConfigurationManager(P plugin) {
        this.plugin = plugin;
    }

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