package fr.robie.messageflow.configuration.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface for managing language configurations in MessageFlow.
 * <p>
 * Implementations define how languages are stored, accessed, and switched.
 *
 * @param <E> the type used to represent languages (e.g., String or an Enum)
 */
public interface LanguageConfiguration<E> {
    
    /**
     * Gets all registered language entries with their file paths.
     *
     * @return a list of language entries
     */
    @NotNull
    List<LanguageEntry> getLanguagesEntries();

    /**
     * Gets the default language for this configuration.
     *
     * @return the default language
     */
    @NotNull
    E getDefaultLanguage();

    /**
     * Gets the currently active language.
     *
     * @return the active language
     */
    @NotNull
    E getActiveLanguage();

    /**
     * Normalizes a language identifier to a consistent format.
     *
     * @param language the language to normalize
     * @return the normalized language string
     */
    String getNormalizedLanguage(@NotNull E language);

    /**
     * Gets the relative file path for a language.
     *
     * @param language the language to get the path for
     * @return the relative file path, or null if not found
     */
    @Nullable
    String getRelativePath(@NotNull E language);

    /**
     * Sets the currently active language.
     *
     * @param language the language to activate
     */
    void setActiveLanguage(@NotNull E language);
}
