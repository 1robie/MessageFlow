package fr.robie.messageflow.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NormalLanguageConfiguration implements LanguageConfiguration<String> {
    private final Map<String, LanguageEntry> languagesEntries = new HashMap<>();

    private final String defaultLanguage;

    private String activeLanguage;

    public NormalLanguageConfiguration(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        this.activeLanguage = defaultLanguage;
    }

    public void addLanguage(@NotNull String languageCode, @NotNull String relativePath) {
        String normalizedLanguage = this.getNormalizedLanguage(languageCode);
        this.languagesEntries.put(normalizedLanguage, new LanguageEntry(normalizedLanguage, relativePath));
    }

    @Override
    public @NotNull List<LanguageEntry> getLanguagesEntries() {
        return this.languagesEntries.values().stream().toList();
    }

    @Override
    public @NonNull String getDefaultLanguage() {
        return this.defaultLanguage;
    }

    @Override
    public @NonNull String getActiveLanguage() {
        return this.activeLanguage;
    }

    @Override
    public String getNormalizedLanguage(String languageCode) {
        if (languageCode.isEmpty()) {
            return "default";
        }
        languageCode = languageCode.replace('-', '_');
        return languageCode.toLowerCase(Locale.ROOT);
    }

    @Override
    @Nullable
    public String getRelativePath(String language) {
        LanguageEntry languageEntry = this.languagesEntries.get(language);
        if (languageEntry == null) {
            return null;
        }
        return languageEntry.path();
    }

    @Override
    public void setActiveLanguage(String language) {
        this.activeLanguage = language;
    }
}
