package fr.robie.messageflow.configuration.lang;

import com.google.common.base.Preconditions;
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
        Preconditions.checkNotNull(availableLanguages, "Available languages class cannot be null");
        Preconditions.checkNotNull(defaultLanguage, "Default language cannot be null");
        this.availableLanguages = availableLanguages;
        this.defaultLanguage = defaultLanguage;
        this.activeLanguage = defaultLanguage;
    }

    /**
     * Sets the format string for language file paths.
     * The %s placeholder will be replaced by the lowercase enum name.
     *
     * @param languagePathFormat the path format string
     * @return this instance for fluent chaining
     */
    public EnumLanguageConfiguration<E> languagePathFormat(@NotNull String languagePathFormat) {
        Preconditions.checkNotNull(languagePathFormat, "Language path format cannot be null");
        this.languagePathFormat = languagePathFormat;
        return this;
    }

    /**
     * Gets the enum class representing available languages.
     *
     * @return the available languages enum class
     */
    public Class<E> getAvailableLanguages() {
        return this.availableLanguages;
    }

    /**
     * Gets the default language.
     *
     * @return the default language
     */
    public @NotNull E getDefaultLanguage() {
        return this.defaultLanguage;
    }

    /**
     * Gets the currently active language.
     *
     * @return the active language
     */
    public @NotNull E getActiveLanguage() {
        return this.activeLanguage;
    }

    /**
     * Gets the language file path format string.
     *
     * @return the path format string
     */
    public @NotNull String getLanguagePathFormat() {
        return this.languagePathFormat;
    }

    @Override
    public String getNormalizedLanguage(@NotNull E language) {
        Preconditions.checkNotNull(language, "Language cannot be null");
        return language.name().toLowerCase();
    }

    @Override
    public String getRelativePath(@NotNull E language) {
        Preconditions.checkNotNull(language, "Language cannot be null");
        return String.format(this.languagePathFormat, language.name().toLowerCase());
    }

    @Override
    public void setActiveLanguage(@NotNull E language) {
        Preconditions.checkNotNull(language, "Language cannot be null");
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
