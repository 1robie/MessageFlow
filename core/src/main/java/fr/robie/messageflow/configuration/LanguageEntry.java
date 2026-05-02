package fr.robie.messageflow.configuration;

import org.jetbrains.annotations.NotNull;

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
