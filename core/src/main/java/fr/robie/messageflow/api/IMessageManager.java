package fr.robie.messageflow.api;

import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.model.Message;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for managing messages, translations, and their lifecycle.
 *
 * @param <T> the type of the plugin using this manager
 */
public interface IMessageManager<T extends Plugin> {

    /**
     * Gets the message formatter used by this manager.
     *
     * @return the message formatter
     */
    @NotNull MessageFormatter<T, ?> formatter();

    /**
     * Reloads all registered language files, updating keys if necessary.
     */
    void reload();

    /**
     * Loads (or reloads) a language file and makes it active.
     *
     * @param languageCode the language code to load (e.g., "en_us")
     */
    void loadLanguage(@NotNull String languageCode);

    /**
     * Gets the currently active language code.
     *
     * @return the active language code
     */
    @NotNull String activeLanguage();

    /**
     * Returns the parsed message entries for the given key.
     *
     * @param message the message key to resolve
     * @return the list of message adapters for the given message
     */
    @NotNull List<MessageTypeAdapter> resolve(@NotNull Message message);
}
