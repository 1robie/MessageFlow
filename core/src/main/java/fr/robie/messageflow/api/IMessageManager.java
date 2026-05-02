package fr.robie.messageflow.api;

import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.model.Message;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IMessageManager<T extends Plugin> {

    @NotNull MessageFormatter<T, ?> formatter();

    void reload();

    /**
     * Loads (or reloads) a language file and makes it active.
     */
    void loadLanguage(@NotNull String languageCode);

    @NotNull String activeLanguage();

    /**
     * Returns the parsed message entries for the given key.
     */
    @NotNull List<MessageTypeAdapter> resolve(@NotNull Message message);
}
