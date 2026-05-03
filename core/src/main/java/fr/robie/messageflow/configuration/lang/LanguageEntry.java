package fr.robie.messageflow.configuration.lang;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single language entry with its identifier and file path.
 *
 * @param language the language identifier
 * @param path     the relative file path to the language file
 */
public record LanguageEntry(String language, String path) {
    public LanguageEntry(@NotNull String language, @NotNull String path) {
        this.language = language;
        this.path = path;
    }

    @Override
    @NotNull
    public String language() {
        return this.language;
    }

    @Override
    @NotNull
    public String path() {
        return this.path;
    }

}
