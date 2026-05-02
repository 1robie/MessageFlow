package fr.robie.messageflow.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents the configuration options for the MessageFlow library.
 * Allows customizing how language files are handled and how the message cache is built.
 */
public class ConfigurationOptions {
    private final Map<String, String> languageToRelativeFile = new LinkedHashMap<>();
    private @Nullable String defaultLanguage = null;
    private boolean autoCreateFiles = true;
    private boolean autoAddMissingKeys = true;
    private boolean autoRemoveObsoleteKeys = false;
    private boolean backupBeforeRemovingObsoleteKeys = true;
    private @NotNull String backupFolder = "messageflow/backup";

    private long cacheMaximumSize = 512;
    private long cacheExpireAfterAccessMinutes = 10;
    private long cacheExpireAfterWriteMinutes = -1;
    private int cacheInitialCapacity = -1;
    private int cacheConcurrencyLevel = -1;
    private boolean cacheRecordStats = false;
    private boolean cacheSoftValues = false;

    public ConfigurationOptions() {
    }

    /**
     * Creates a new ConfigurationOptions instance for a single language file.
     * The language code becomes {@code "default"}.
     *
     * @param relativeFile the path to the language file relative to the plugin data folder
     * @return a new ConfigurationOptions instance
     */
    public static @NotNull ConfigurationOptions singleFile(@NotNull String relativeFile) {
        return new ConfigurationOptions()
                .clearLanguages()
                .addLanguage("default", relativeFile)
                .defaultLanguage("default");
    }

    /**
     * Adds a language file mapping.
     *
     * @param languageCode the code of the language (e.g., "en_us")
     * @param relativeFile the path to the language file relative to the plugin data folder
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions addLanguage(@NotNull String languageCode, @NotNull String relativeFile) {
        String lang = normalizeLanguage(languageCode);
        this.languageToRelativeFile.put(lang, Objects.requireNonNull(relativeFile, "relativeFile"));
        if (this.defaultLanguage == null) {
            this.defaultLanguage = lang;
        }
        return this;
    }

    /**
     * Clears all registered language files.
     *
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions clearLanguages() {
        this.languageToRelativeFile.clear();
        this.defaultLanguage = null;
        return this;
    }

    /**
     * Sets the default language to use when the requested language is not found.
     *
     * @param languageCode the code of the default language
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions defaultLanguage(@NotNull String languageCode) {
        this.defaultLanguage = normalizeLanguage(languageCode);
        return this;
    }

    /**
     * Sets whether missing language files should be automatically created.
     *
     * @param enabled {@code true} to enable automatic file creation
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions autoCreateFiles(boolean enabled) {
        this.autoCreateFiles = enabled;
        return this;
    }

    /**
     * Sets whether missing keys in existing language files should be automatically added.
     *
     * @param enabled {@code true} to enable automatic adding of missing keys
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions autoAddMissingKeys(boolean enabled) {
        this.autoAddMissingKeys = enabled;
        return this;
    }

    /**
     * Sets whether obsolete keys (keys not defined in the message provider) should be automatically removed.
     *
     * @param enabled {@code true} to enable automatic removal of obsolete keys
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions autoRemoveObsoleteKeys(boolean enabled) {
        this.autoRemoveObsoleteKeys = enabled;
        return this;
    }

    /**
     * Sets whether a backup should be created before removing obsolete keys.
     *
     * @param enabled {@code true} to enable backups before removing keys
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions backupBeforeRemovingObsoleteKeys(boolean enabled) {
        this.backupBeforeRemovingObsoleteKeys = enabled;
        return this;
    }

    /**
     * Sets the folder where backups will be stored, relative to the plugin data folder.
     *
     * @param relativeFolder the relative path to the backup folder
     * @return this instance for fluent chaining
     */
    public @NotNull ConfigurationOptions backupFolder(@NotNull String relativeFolder) {
        this.backupFolder = Objects.requireNonNull(relativeFolder, "relativeFolder");
        return this;
    }

    /**
     * Gets an unmodifiable map of registered language codes to their relative file paths.
     *
     * @return a map of language files
     */
    public @NotNull Map<String, String> languageFiles() {
        return Collections.unmodifiableMap(this.languageToRelativeFile);
    }

    /**
     * Gets the code of the default language.
     *
     * @return the default language code
     * @throws IllegalStateException if no default language has been set
     */
    public @NotNull String defaultLanguage() {
        if (this.defaultLanguage == null) {
            throw new IllegalStateException("No defaultLanguage set. Call addLanguage(...) or use singleFile(...).");
        }
        return this.defaultLanguage;
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
    public @NotNull ConfigurationOptions cacheMaximumSize(long maximumSize) {
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
    public @NotNull ConfigurationOptions cacheExpireAfterAccessMinutes(long minutes) {
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
    public @NotNull ConfigurationOptions cacheExpireAfterWriteMinutes(long minutes) {
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
    public @NotNull ConfigurationOptions cacheInitialCapacity(int initialCapacity) {
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
    public @NotNull ConfigurationOptions cacheConcurrencyLevel(int concurrencyLevel) {
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
    public @NotNull ConfigurationOptions cacheRecordStats(boolean recordStats) {
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
    public @NotNull ConfigurationOptions cacheSoftValues(boolean softValues) {
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
     * Normalizes a language code (e.g., converts to lowercase and replaces '-' with '_').
     *
     * @param languageCode the language code to normalize
     * @return the normalized language code
     */
    public static @NotNull String normalizeLanguage(@NotNull String languageCode) {
        String raw = Objects.requireNonNull(languageCode, "languageCode").trim();
        if (raw.isEmpty()) {
            return "default";
        }
        // allow "en_US", "en-US", "fr", etc.
        raw = raw.replace('-', '_');
        // keep case stable for file keys / configs
        return raw.toLowerCase(Locale.ROOT);
    }
}
