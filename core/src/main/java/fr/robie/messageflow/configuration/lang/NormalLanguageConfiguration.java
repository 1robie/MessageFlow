package fr.robie.messageflow.configuration.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Language configuration implementation that uses string-based language codes.
 * <p>
 * Languages must be explicitly added with their corresponding file paths.
 * Language codes are normalized to lowercase with underscores as separators.
 */
public class NormalLanguageConfiguration implements LanguageConfiguration<String> {
    private final Map<String, LanguageEntry> languagesEntries = new HashMap<>();

    private final String defaultLanguage;

    private String activeLanguage;

    /**
     * Creates a new NormalLanguageConfiguration with the specified default language.
     *
     * @param defaultLanguage the default language code
     */
    public NormalLanguageConfiguration(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        this.activeLanguage = defaultLanguage;
    }

    /**
     * Adds a language with its corresponding file path to this configuration.
     *
     * @param languageCode the language code (will be normalized)
     * @param relativePath the relative path to the language file
     */
    public void addLanguage(@NotNull String languageCode, @NotNull String relativePath) {
        String normalizedLanguage = this.getNormalizedLanguage(languageCode);
        this.languagesEntries.put(normalizedLanguage, new LanguageEntry(normalizedLanguage, relativePath));
    }

    @Override
    public @NotNull List<LanguageEntry> getLanguagesEntries() {
        return this.languagesEntries.values().stream().toList();
    }

    @Override
    public @NotNull String getDefaultLanguage() {
        return this.defaultLanguage;
    }

    @Override
    public @NotNull String getActiveLanguage() {
        return this.activeLanguage;
    }

    @Override
    public String getNormalizedLanguage(@NotNull String languageCode) {
        if (languageCode.isEmpty()) {
            return "default";
        }
        languageCode = languageCode.replace('-', '_');
        return languageCode.toLowerCase(Locale.ROOT);
    }

    @Override
    @Nullable
    public String getRelativePath(@NotNull String language) {
        LanguageEntry languageEntry = this.languagesEntries.get(language);
        if (languageEntry == null) {
            return null;
        }
        return languageEntry.path();
    }

    @Override
    public void setActiveLanguage(@NotNull String language) {
        this.activeLanguage = language;
    }
}
