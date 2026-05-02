package fr.robie.messageflow.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ConfigurationOptions {
    private final Map<String, String> languageToRelativeFile = new LinkedHashMap<>();
    private @Nullable String defaultLanguage = null;
    private boolean autoCreateFiles = true;
    private boolean autoAddMissingKeys = true;
    private boolean autoRemoveObsoleteKeys = false;
    private boolean backupBeforeRemovingObsoleteKeys = true;
    private @NotNull String backupFolder = "messageflow/backup";

    public ConfigurationOptions() {
    }

    /**
     * Single file mode. The language code becomes {@code "default"}.
     */
    public static @NotNull ConfigurationOptions singleFile(@NotNull String relativeFile) {
        return new ConfigurationOptions()
                .clearLanguages()
                .addLanguage("default", relativeFile)
                .defaultLanguage("default");
    }

    public @NotNull ConfigurationOptions addLanguage(@NotNull String languageCode, @NotNull String relativeFile) {
        String lang = normalizeLanguage(languageCode);
        this.languageToRelativeFile.put(lang, Objects.requireNonNull(relativeFile, "relativeFile"));
        if (this.defaultLanguage == null) {
            this.defaultLanguage = lang;
        }
        return this;
    }

    public @NotNull ConfigurationOptions clearLanguages() {
        this.languageToRelativeFile.clear();
        this.defaultLanguage = null;
        return this;
    }

    public @NotNull ConfigurationOptions defaultLanguage(@NotNull String languageCode) {
        this.defaultLanguage = normalizeLanguage(languageCode);
        return this;
    }

    public @NotNull ConfigurationOptions autoCreateFiles(boolean enabled) {
        this.autoCreateFiles = enabled;
        return this;
    }

    public @NotNull ConfigurationOptions autoAddMissingKeys(boolean enabled) {
        this.autoAddMissingKeys = enabled;
        return this;
    }

    public @NotNull ConfigurationOptions autoRemoveObsoleteKeys(boolean enabled) {
        this.autoRemoveObsoleteKeys = enabled;
        return this;
    }

    public @NotNull ConfigurationOptions backupBeforeRemovingObsoleteKeys(boolean enabled) {
        this.backupBeforeRemovingObsoleteKeys = enabled;
        return this;
    }

    /**
     * Folder relative to plugin data folder.
     */
    public @NotNull ConfigurationOptions backupFolder(@NotNull String relativeFolder) {
        this.backupFolder = Objects.requireNonNull(relativeFolder, "relativeFolder");
        return this;
    }

    public @NotNull Map<String, String> languageFiles() {
        return Collections.unmodifiableMap(this.languageToRelativeFile);
    }

    public @NotNull String defaultLanguage() {
        if (this.defaultLanguage == null) {
            throw new IllegalStateException("No defaultLanguage set. Call addLanguage(...) or use singleFile(...).");
        }
        return this.defaultLanguage;
    }

    public boolean autoCreateFiles() {
        return this.autoCreateFiles;
    }

    public boolean autoAddMissingKeys() {
        return this.autoAddMissingKeys;
    }

    public boolean autoRemoveObsoleteKeys() {
        return this.autoRemoveObsoleteKeys;
    }

    public boolean backupBeforeRemovingObsoleteKeys() {
        return this.backupBeforeRemovingObsoleteKeys;
    }

    public @NotNull String backupFolder() {
        return this.backupFolder;
    }

    public static @NotNull String normalizeLanguage(@NotNull String languageCode) {
        String raw = Objects.requireNonNull(languageCode, "languageCode").trim();
        if (raw.isEmpty()) return "default";
        // allow "en_US", "en-US", "fr", etc.
        raw = raw.replace('-', '_');
        // keep case stable for file keys / configs
        return raw.toLowerCase(Locale.ROOT);
    }
}
