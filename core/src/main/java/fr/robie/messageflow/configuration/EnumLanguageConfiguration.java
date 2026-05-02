package fr.robie.messageflow.configuration;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class EnumLanguageConfiguration<E extends Enum<E>> implements LanguageConfiguration<E> {

    private final Class<E> availableLanguages;

    private final E defaultLanguage;

    private final String languagePathFormat = "lang/%s.yml";

    private E activeLanguage;

    public EnumLanguageConfiguration(Class<E> availableLanguages, E defaultLanguage) {
        this.availableLanguages = availableLanguages;
        this.defaultLanguage = defaultLanguage;
        this.activeLanguage = defaultLanguage;
    }

    public Class<E> getAvailableLanguages() {
        return this.availableLanguages;
    }

    public @NonNull E getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public @NonNull E getActiveLanguage() {
        return this.activeLanguage;
    }

    @Override
    public String getNormalizedLanguage(E language) {
        return language.name().toLowerCase();
    }

    @Override
    public String getRelativePath(E language) {
        return String.format(this.languagePathFormat, language.name().toLowerCase());
    }

    @Override
    public void setActiveLanguage(E language) {
        this.activeLanguage = language;
    }

    @Override
    public @NotNull List<LanguageEntry> getLanguagesEntries() {
        List<LanguageEntry> entries = new ArrayList<>();
        for (E language : this.availableLanguages.getEnumConstants()) {
            String path = String.format(this.languagePathFormat, language.name().toLowerCase());
            entries.add(new LanguageEntry(language.name(), path));
        }
        return entries;
    }
}
