package fr.robie.messageflow.configuration;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.configuration.lang.LanguageConfiguration;
import fr.robie.messageflow.configuration.lang.NormalLanguageConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the configuration options for the MessageFlow library.
 * Allows customizing how language files are handled and how the message cache is built.
 */
public record ConfigurationOptions<E>(LanguageConfiguration<E> languageConfiguration) {
    /**
     * Creates a new ConfigurationOptions with the specified language configuration.
     *
     * @param languageConfiguration the language configuration to use
     */
    public ConfigurationOptions(@NotNull LanguageConfiguration<E> languageConfiguration) {
        Preconditions.checkNotNull(languageConfiguration, "languageConfiguration cannot be null");
        this.languageConfiguration = languageConfiguration;
    }

    /**
     * Sets the date format to use for backup file names. This is used when creating backups before removing obsolete keys.
     *
     * @param format the date format string (e.g. "yyyy-MM-dd_HH-mm-ss")
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#BACKUP_DATE_FORMAT} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> backupDateFormat(@NotNull String format) {
        Preconditions.checkNotNull(format, "backupDateFormat cannot be null");
        ConfigurationManager.Setting.BACKUP_DATE_FORMAT.setDefaultValue(format);
        return this;
    }

    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull String backupDateFormat() {
        return ConfigurationManager.Setting.BACKUP_DATE_FORMAT.getDefaultValue();
    }

    /**
     * Sets whether missing language files should be automatically created.
     *
     * @param enabled {@code true} to enable automatic file creation
     * @return this instance for fluent chaining
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> autoCreateFiles(boolean enabled) {
        ConfigurationManager.Setting.SYNC_AUTO_CREATE.setDefaultValue(enabled);
        return this;
    }

    /**
     * Sets whether missing keys in existing language files should be automatically added.
     *
     * @param enabled {@code true} to enable automatic adding of missing keys
     * @return this instance for fluent chaining
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> autoAddMissingKeys(boolean enabled) {
        ConfigurationManager.Setting.SYNC_AUTO_ADD_MISSING.setDefaultValue(enabled);
        return this;
    }

    /**
     * Sets whether obsolete keys (keys not defined in the message provider) should be automatically removed.
     *
     * @param enabled {@code true} to enable automatic removal of obsolete keys
     * @return this instance for fluent chaining
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> autoRemoveObsoleteKeys(boolean enabled) {
        ConfigurationManager.Setting.SYNC_AUTO_REMOVE_OBSOLETE.setDefaultValue(enabled);
        return this;
    }

    /**
     * Sets whether a backup should be created before removing obsolete keys.
     *
     * @param enabled {@code true} to enable backups before removing keys
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#BACKUP_ENABLED} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> backupBeforeRemovingObsoleteKeys(boolean enabled) {
        ConfigurationManager.Setting.BACKUP_ENABLED.setDefaultValue(enabled);
        return this;
    }

    /**
     * Sets the folder where backups will be stored, relative to the plugin data folder.
     *
     * @param relativeFolder the relative path to the backup folder
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#BACKUP_DIRECTORY} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> backupFolder(@NotNull String relativeFolder) {
        Preconditions.checkNotNull(relativeFolder, "relativeFolder cannot be null");
        ConfigurationManager.Setting.BACKUP_DIRECTORY.setDefaultValue(relativeFolder);
        return this;
    }

    /**
     * Checks if automatic file creation is enabled.
     *
     * @return {@code true} if enabled
     * @deprecated Use {@link ConfigurationManager.Setting#SYNC_AUTO_CREATE} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public boolean autoCreateFiles() {
        return ConfigurationManager.Setting.SYNC_AUTO_CREATE.getDefaultValue();
    }

    /**
     * Checks if automatic adding of missing keys is enabled.
     *
     * @return {@code true} if enabled
     * @deprecated Use {@link ConfigurationManager.Setting#SYNC_AUTO_ADD_MISSING} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public boolean autoAddMissingKeys() {
        return ConfigurationManager.Setting.SYNC_AUTO_ADD_MISSING.getDefaultValue();
    }

    /**
     * Checks if automatic removal of obsolete keys is enabled.
     *
     * @return {@code true} if enabled
     * @deprecated Use {@link ConfigurationManager.Setting#SYNC_AUTO_REMOVE_OBSOLETE} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public boolean autoRemoveObsoleteKeys() {
        return ConfigurationManager.Setting.SYNC_AUTO_REMOVE_OBSOLETE.getDefaultValue();
    }

    /**
     * Checks if backups before removing obsolete keys are enabled.
     *
     * @return {@code true} if enabled
     * @deprecated Use {@link ConfigurationManager.Setting#BACKUP_ENABLED} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public boolean backupBeforeRemovingObsoleteKeys() {
        return ConfigurationManager.Setting.BACKUP_ENABLED.getDefaultValue();
    }

    /**
     * Gets the backup folder path relative to the plugin data folder.
     *
     * @return the relative backup folder path
     * @deprecated Use {@link ConfigurationManager.Setting#BACKUP_DIRECTORY} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull String backupFolder() {
        return ConfigurationManager.Setting.BACKUP_DIRECTORY.getDefaultValue();
    }

    /**
     * Gets the logger prefix to use for log messages. If null, the library will use the plugin name + version as the prefix.
     *
     * @return the logger prefix, or null to use the default plugin name + version prefix
     * @deprecated Use {@link ConfigurationManager.Setting#LEGACY_LOGGER_PREFIX} or {@link ConfigurationManager.Setting#ADVENTURE_LOGGER_PREFIX} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @Nullable String loggerPrefix() {
        return ConfigurationManager.Setting.LEGACY_LOGGER_PREFIX.getDefaultValue();
    }

    /**
     * Sets the logger prefix to use for log messages. If set to null, the library will use the plugin name + version as the prefix.
     *
     * @param prefix the logger prefix to use, or null to use the default plugin name + version prefix
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#LEGACY_LOGGER_PREFIX} or {@link ConfigurationManager.Setting#ADVENTURE_LOGGER_PREFIX} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> loggerPrefix(@Nullable String prefix) {
        ConfigurationManager.Setting.LEGACY_LOGGER_PREFIX.setDefaultValue(prefix);
        ConfigurationManager.Setting.ADVENTURE_LOGGER_PREFIX.setDefaultValue(prefix);
        return this;
    }

    /**
     * Sets the maximum number of entries to keep in the message cache.
     *
     * @param maximumSize the maximum cache size
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_MAX_SIZE} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheMaximumSize(long maximumSize) {
        ConfigurationManager.Setting.MESSAGE_CACHE_MAX_SIZE.setDefaultValue(maximumSize);
        return this;
    }

    /**
     * Gets the maximum number of entries to keep in the message cache.
     *
     * @return the maximum cache size
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_MAX_SIZE} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public long cacheMaximumSize() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_MAX_SIZE.getDefaultValue();
    }

    /**
     * Sets the time in minutes after last access when a cache entry should expire.
     *
     * @param minutes the expiration time in minutes
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_EXPIRE_AFTER_ACCESS} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheExpireAfterAccessMinutes(long minutes) {
        ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_ACCESS.setDefaultValue(minutes);
        return this;
    }

    /**
     * Gets the time in minutes after last access when a cache entry should expire.
     *
     * @return the expiration time in minutes
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_EXPIRE_AFTER_ACCESS} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public long cacheExpireAfterAccessMinutes() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_ACCESS.getDefaultValue();
    }

    /**
     * Sets the time in minutes after creation when a cache entry should expire.
     *
     * @param minutes the expiration time in minutes
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_EXPIRE_AFTER_WRITE} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheExpireAfterWriteMinutes(long minutes) {
        ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_WRITE.setDefaultValue(minutes);
        return this;
    }

    /**
     * Gets the time in minutes after creation when a cache entry should expire.
     *
     * @return the expiration time in minutes
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_EXPIRE_AFTER_WRITE} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public long cacheExpireAfterWriteMinutes() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_EXPIRE_AFTER_WRITE.getDefaultValue();
    }

    /**
     * Sets the initial capacity of the message cache.
     *
     * @param initialCapacity the initial capacity
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_INITIAL_CAPACITY} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheInitialCapacity(int initialCapacity) {
        ConfigurationManager.Setting.MESSAGE_CACHE_INITIAL_CAPACITY.setDefaultValue(initialCapacity);
        return this;
    }

    /**
     * Gets the initial capacity of the message cache.
     *
     * @return the initial capacity
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_INITIAL_CAPACITY} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public int cacheInitialCapacity() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_INITIAL_CAPACITY.getDefaultValue();
    }

    /**
     * Sets the concurrency level for the message cache.
     *
     * @param concurrencyLevel the concurrency level
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_CONCURRENCY_LEVEL} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheConcurrencyLevel(int concurrencyLevel) {
        ConfigurationManager.Setting.MESSAGE_CACHE_CONCURRENCY_LEVEL.setDefaultValue(concurrencyLevel);
        return this;
    }

    /**
     * Gets the concurrency level for the message cache.
     *
     * @return the concurrency level
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_CONCURRENCY_LEVEL} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public int cacheConcurrencyLevel() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_CONCURRENCY_LEVEL.getDefaultValue();
    }

    /**
     * Sets whether cache statistics should be recorded.
     *
     * @param recordStats {@code true} to record statistics
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_RECORD_STATS} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheRecordStats(boolean recordStats) {
        ConfigurationManager.Setting.MESSAGE_CACHE_RECORD_STATS.setDefaultValue(recordStats);
        return this;
    }

    /**
     * Checks if cache statistics recording is enabled.
     *
     * @return {@code true} if enabled
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_RECORD_STATS} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public boolean cacheRecordStats() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_RECORD_STATS.getDefaultValue();
    }

    /**
     * Sets whether soft references should be used for cache values.
     *
     * @param softValues {@code true} to use soft references
     * @return this instance for fluent chaining
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_SOFT_VALUES} and call {@link ConfigurationManager.Setting#setDefaultValue(Object)}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public @NotNull ConfigurationOptions<E> cacheSoftValues(boolean softValues) {
        ConfigurationManager.Setting.MESSAGE_CACHE_SOFT_VALUES.setDefaultValue(softValues);
        return this;
    }

    /**
     * Checks if soft references for cache values are enabled.
     *
     * @return {@code true} if enabled
     * @deprecated Use {@link ConfigurationManager.Setting#MESSAGE_CACHE_SOFT_VALUES} and call {@link ConfigurationManager.Setting#getDefaultValue()}
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public boolean cacheSoftValues() {
        return ConfigurationManager.Setting.MESSAGE_CACHE_SOFT_VALUES.getDefaultValue();
    }

    /**
     * Gets the language configuration used by this MessageFlow instance.
     *
     * @return the language configuration
     */
    @Override
    public LanguageConfiguration<E> languageConfiguration() {
        return this.languageConfiguration;
    }

    /**
     * Creates a ConfigurationOptions with a single language file using the default configuration.
     *
     * @param fileName the name of the single language file
     * @return a new ConfigurationOptions instance configured for a single file
     * @deprecated Use {@link NormalLanguageConfiguration} directly
     */
    @Deprecated(since = "0.0.5", forRemoval = true)
    public static ConfigurationOptions<String> singleFile(String fileName) {
        NormalLanguageConfiguration normalLanguageConfiguration = new NormalLanguageConfiguration("default");
        normalLanguageConfiguration.addLanguage("default", fileName);
        return new ConfigurationOptions<>(normalLanguageConfiguration);
    }
}
