package fr.robie.messageflow.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LanguageConfiguration<E> {

    @NotNull
    List<LanguageEntry> getLanguagesEntries();

    @NotNull
    E getDefaultLanguage();

    @NotNull
    E getActiveLanguage();

    String getNormalizedLanguage(E language);

    @Nullable
    String getRelativePath(E language);

    void setActiveLanguage(E language);
}
