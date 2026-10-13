package fr.robie.messageflow.api;

import fr.robie.messageflow.model.Message;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Picks the language of each recipient and the text of a message in that language. Set on a formatter by a
 * manager with per-player languages; without one, every recipient gets {@link Message#loaded()}.
 */
public interface MessageLocalizer {
    /**
     * Returns the language a recipient reads.
     *
     * @param viewer the recipient; null or not a player means the default language
     * @return a language code known to this localizer
     */
    @NotNull String language(@Nullable CommandSender viewer);

    /**
     * Returns the language for a client locale.
     *
     * @param locale the client locale
     * @return a language code known to this localizer
     */
    @NotNull String language(@NotNull Locale locale);

    /**
     * Returns the default language, used for the console and for anything without a player.
     *
     * @return the default language code
     */
    @NotNull String defaultLanguage();

    /**
     * Returns a message in a language, falling back to the default language when that language has no entry for it.
     *
     * @param message  the message definition
     * @param language a language code from {@link #language(CommandSender)}
     * @return the message's adapters in that language
     */
    @NotNull List<? extends MessageTypeAdapter> adapters(@NotNull Message message, @NotNull String language);
}
