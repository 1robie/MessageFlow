package fr.robie.messageflow.api;

import fr.robie.messageflow.configuration.lang.LanguageConfiguration;
import fr.robie.messageflow.formatter.MessageFormatter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for managing messages, translations, and their lifecycle.
 *
 * @param <T> the type of the plugin using this manager
 */
public interface IMessageManager<T extends Plugin, E> {

    /**
     * Gets the message formatter used by this message manager. The formatter is responsible for adapting
     * messages for presentation depending on features like Adventure or Legacy formatting.
     *
     * @return the message formatter instance
     */
    @NotNull MessageFormatter<T, ?> formatter();

    /**
     * Gets the language configuration used by this message manager. The language configuration defines
     * available languages, their file paths, and the active language.
     *
     * @return the language configuration instance
     */
    @NotNull LanguageConfiguration<E> languageConfiguration();

    /**
     * Alias for {@link #loadLanguage(E)}, provided for convenience.
     *
     * @param language the language code to set as active (e.g., "en_us")
     */
    default void setActiveLanguage(@NotNull E language) {
        this.loadLanguage(language);
    }

    @NotNull fr.robie.messageflow.configuration.ConfigurationManager<T> configurationManager();

    /**
     * Creates a new fluent message builder for creating and sending messages on-the-fly.
     *
     * @return a new message builder instance
     */
    @NotNull IMessageBuilder builder();

    /**
     * Reloads all registered language files, updating keys if necessary.
     */
    void reload();

    /**
     * Loads (or reloads) a language file and makes it active.
     *
     * @param language the language code to load (e.g., "en_us")
     */
    void loadLanguage(@NotNull E language);
}
