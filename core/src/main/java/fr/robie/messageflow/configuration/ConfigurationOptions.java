package fr.robie.messageflow.configuration;

import fr.robie.messageflow.configuration.lang.LanguageConfiguration;
import fr.robie.messageflow.configuration.lang.NormalLanguageConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents the configuration options for the MessageFlow library.
 * Allows customizing how language files are handled and how the message cache is built.
 */
public class ConfigurationOptions<E> {
    private final LanguageConfiguration<E> languageConfiguration;

    private boolean autoCreateFiles = true;
    private boolean autoAddMissingKeys = true;
    private boolean autoRemoveObsoleteKeys = false;
    private boolean backupBeforeRemovingObsoleteKeys = true;
    private @NotNull String backupFolder = "messageflow/backup";

    private @NotNull String backupDateFormat = "yyyy-MM-dd_HH-mm-ss";

    private long cacheMaximumSize = 512;
    private long cacheExpireAfterAccessMinutes = 10;
    private long cacheExpireAfterWriteMinutes = -1;
    private int cacheInitialCapacity = -1;
    private int cacheConcurrencyLevel = -1;
    private boolean cacheRecordStats = false;
    private boolean cacheSoftValues = false;

    public @NotNull String backupDateFormat() {
        return this.backupDateFormat;
    }

    /**
     * Creates a new ConfigurationOptions with the specified language configuration.
     *
     * @param languageConfiguration the language configuration to use
     */
    public ConfigurationOptions(@NotNull LanguageConfiguration<E> languageConfiguration) {
        this.languageConfiguration = Objects.requireNonNull(languageConfiguration, "languageConfiguration");
    }

    public @NotNull ConfigurationOptions<E> backupDateFormat(@NotNull String format) {
        this.backupDateFormat = Objects.requireNonNull(format, "backupDateFormat");
        return this;
    }

    /**
     * Sets whether missing language files should be automatically created.
     *
     * @param enabled {@code true} to enable automatic file creation
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> autoCreateFiles(boolean enabled) {
        this.autoCreateFiles = enabled;
        return this;
    }

    /**
     * Sets whether missing keys in existing language files should be automatically added.
     *
     * @param enabled {@code true} to enable automatic adding of missing keys
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> autoAddMissingKeys(boolean enabled) {
        this.autoAddMissingKeys = enabled;
        return this;
    }

    /**
     * Sets whether obsolete keys (keys not defined in the message provider) should be automatically removed.
     *
     * @param enabled {@code true} to enable automatic removal of obsolete keys
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> autoRemoveObsoleteKeys(boolean enabled) {
        this.autoRemoveObsoleteKeys = enabled;
        return this;
    }

    /**
     * Sets whether a backup should be created before removing obsolete keys.
     *
     * @param enabled {@code true} to enable backups before removing keys
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> backupBeforeRemovingObsoleteKeys(boolean enabled) {
        this.backupBeforeRemovingObsoleteKeys = enabled;
        return this;
    }

    /**
     * Sets the folder where backups will be stored, relative to the plugin data folder.
     *
     * @param relativeFolder the relative path to the backup folder
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> backupFolder(@NotNull String relativeFolder) {
        this.backupFolder = Objects.requireNonNull(relativeFolder, "relativeFolder");
        return this;
    }

    /**
     * Checks if automatic file creation is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean autoCreateFiles() {
        return this.autoCreateFiles;
    }

    /**
     * Checks if automatic adding of missing keys is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean autoAddMissingKeys() {
        return this.autoAddMissingKeys;
    }

    /**
     * Checks if automatic removal of obsolete keys is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean autoRemoveObsoleteKeys() {
        return this.autoRemoveObsoleteKeys;
    }

    /**
     * Checks if backups before removing obsolete keys are enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean backupBeforeRemovingObsoleteKeys() {
        return this.backupBeforeRemovingObsoleteKeys;
    }

    /**
     * Gets the backup folder path relative to the plugin data folder.
     *
     * @return the relative backup folder path
     */
    public @NotNull String backupFolder() {
        return this.backupFolder;
    }

    /**
     * Sets the maximum number of entries to keep in the message cache.
     *
     * @param maximumSize the maximum cache size
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheMaximumSize(long maximumSize) {
        this.cacheMaximumSize = maximumSize;
        return this;
    }

    /**
     * Gets the maximum number of entries to keep in the message cache.
     *
     * @return the maximum cache size
     */
    public long cacheMaximumSize() {
        return this.cacheMaximumSize;
    }

    /**
     * Sets the time in minutes after last access when a cache entry should expire.
     *
     * @param minutes the expiration time in minutes
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheExpireAfterAccessMinutes(long minutes) {
        this.cacheExpireAfterAccessMinutes = minutes;
        return this;
    }

    /**
     * Gets the time in minutes after last access when a cache entry should expire.
     *
     * @return the expiration time in minutes
     */
    public long cacheExpireAfterAccessMinutes() {
        return this.cacheExpireAfterAccessMinutes;
    }

    /**
     * Sets the time in minutes after creation when a cache entry should expire.
     *
     * @param minutes the expiration time in minutes
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheExpireAfterWriteMinutes(long minutes) {
        this.cacheExpireAfterWriteMinutes = minutes;
        return this;
    }

    /**
     * Gets the time in minutes after creation when a cache entry should expire.
     *
     * @return the expiration time in minutes
     */
    public long cacheExpireAfterWriteMinutes() {
        return this.cacheExpireAfterWriteMinutes;
    }

    /**
     * Sets the initial capacity of the message cache.
     *
     * @param initialCapacity the initial capacity
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheInitialCapacity(int initialCapacity) {
        this.cacheInitialCapacity = initialCapacity;
        return this;
    }

    /**
     * Gets the initial capacity of the message cache.
     *
     * @return the initial capacity
     */
    public int cacheInitialCapacity() {
        return this.cacheInitialCapacity;
    }

    /**
     * Sets the concurrency level for the message cache.
     *
     * @param concurrencyLevel the concurrency level
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheConcurrencyLevel(int concurrencyLevel) {
        this.cacheConcurrencyLevel = concurrencyLevel;
        return this;
    }

    /**
     * Gets the concurrency level for the message cache.
     *
     * @return the concurrency level
     */
    public int cacheConcurrencyLevel() {
        return this.cacheConcurrencyLevel;
    }

    /**
     * Sets whether cache statistics should be recorded.
     *
     * @param recordStats {@code true} to record statistics
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheRecordStats(boolean recordStats) {
        this.cacheRecordStats = recordStats;
        return this;
    }

    /**
     * Checks if cache statistics recording is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean cacheRecordStats() {
        return this.cacheRecordStats;
    }

    /**
     * Sets whether soft references should be used for cache values.
     *
     * @param softValues {@code true} to use soft references
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions<E> cacheSoftValues(boolean softValues) {
        this.cacheSoftValues = softValues;
        return this;
    }

    /**
     * Checks if soft references for cache values are enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean cacheSoftValues() {
        return this.cacheSoftValues;
    }

    /**
     * Gets the language configuration used by this MessageFlow instance.
     *
     * @return the language configuration
     */
    public LanguageConfiguration<E> languageConfiguration() {
        return this.languageConfiguration;
    }

    /**
     * Creates a ConfigurationOptions with a single language file using the default configuration.
     *
     * @param fileName the name of the single language file
     * @return a new ConfigurationOptions instance configured for a single file
     */
    public static ConfigurationOptions<String> singleFile(String fileName) {
        NormalLanguageConfiguration normalLanguageConfiguration = new NormalLanguageConfiguration("default");
        normalLanguageConfiguration.addLanguage("default", fileName);
        return new ConfigurationOptions<>(normalLanguageConfiguration);
    }
}
