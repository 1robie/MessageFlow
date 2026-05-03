package fr.robie.messageflow.configuration.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Language configuration implementation that uses an enum to define available languages.
 * <p>
 * This configuration automatically generates file paths based on a format string
 * and the enum constant names.
 *
 * @param <E> the enum type representing available languages
 */
public class EnumLanguageConfiguration<E extends Enum<E>> implements LanguageConfiguration<E> {
    private final Class<E> availableLanguages;
    private final E defaultLanguage;
    private String languagePathFormat = "lang/%s.yml";

    private E activeLanguage;

    /**
     * Creates a new EnumLanguageConfiguration with the specified enum class and default language.
     *
     * @param availableLanguages the enum class containing available languages
     * @param defaultLanguage    the default language
     */
    public EnumLanguageConfiguration(@NotNull Class<E> availableLanguages, @NotNull E defaultLanguage) {
        this.availableLanguages = availableLanguages;
        this.defaultLanguage = defaultLanguage;
        this.activeLanguage = defaultLanguage;
    }

    public EnumLanguageConfiguration<E> languagePathFormat(@NotNull String languagePathFormat) {
        this.languagePathFormat = languagePathFormat;
        return this;
    }

    public Class<E> getAvailableLanguages() {
        return this.availableLanguages;
    }

    public @NotNull E getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public @NotNull E getActiveLanguage() {
        return this.activeLanguage;
    }

    public @NotNull String getLanguagePathFormat() {
        return this.languagePathFormat;
    }

    @Override
    public String getNormalizedLanguage(@NotNull E language) {
        return language.name().toLowerCase();
    }

    @Override
    public String getRelativePath(@NotNull E language) {
        return String.format(this.languagePathFormat, language.name().toLowerCase());
    }

    @Override
    public void setActiveLanguage(@NotNull E language) {
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
