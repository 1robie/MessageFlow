package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.MessageLocalizer;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.util.PlatformType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The languages of a manager with per-player languages, as loaded by one reload. Immutable apart from a cache of
 * resolved client locales, so formatters read it from any thread while the next reload builds a new one.
 * <p>
 * A client locale resolves to the first match of: the language with that exact code ({@code fr_ca}), the first
 * registered language of the same language ({@code fr_*}), the default language. A message missing from a
 * language falls back to the default language ({@link Message#loaded()}).
 */
final class Translations implements MessageLocalizer {
    /**
     * Clients choose their locale string: past this many distinct ones, results are no longer cached.
     */
    private static final int MAX_CACHED_LOCALES = 256;

    private final String defaultLanguage;
    private final List<String> languages;
    private final Map<String, Map<String, List<MessageTypeAdapter>>> byLanguage;
    private final Map<String, Message> messages;
    private final Map<String, String> resolved = new ConcurrentHashMap<>();

    /**
     * @param defaultLanguage the default language, whose text is in each message's {@link Message#loaded()}
     * @param languages       every language with a file, default included, in registration order
     * @param byLanguage      for each other language, the messages its file defines, by key
     * @param messages        the registered messages, by key
     */
    Translations(@NotNull String defaultLanguage, @NotNull List<String> languages,
                 @NotNull Map<String, Map<String, List<MessageTypeAdapter>>> byLanguage, @NotNull Map<String, Message> messages) {
        this.defaultLanguage = defaultLanguage;
        this.languages = List.copyOf(languages);
        this.byLanguage = Map.copyOf(byLanguage);
        this.messages = Map.copyOf(messages);
    }

    @Override
    public @NotNull String defaultLanguage() {
        return this.defaultLanguage;
    }

    @Override
    public @NotNull String language(@Nullable CommandSender viewer) {
        if (!(viewer instanceof Player player)) {
            return this.defaultLanguage;
        }
        return this.resolve(PlatformType.hasComponent() ? player.locale().toString() : player.getLocale());
    }

    @Override
    public @NotNull String language(@NotNull Locale locale) {
        return this.resolve(locale.toString());
    }

    @Override
    public @NotNull List<? extends MessageTypeAdapter> adapters(@NotNull Message message, @NotNull String language) {
        if (!language.equals(this.defaultLanguage) && this.messages.get(message.key()) == message) {
            Map<String, List<MessageTypeAdapter>> translated = this.byLanguage.get(language);
            List<MessageTypeAdapter> adapters = translated != null ? translated.get(message.key()) : null;
            if (adapters != null) {
                return adapters;
            }
        }
        return message.loaded().isEmpty() ? message.defaults() : message.loaded();
    }

    /**
     * Returns the registered message with a key.
     *
     * @param key the message key
     * @return the message, or null
     */
    @Nullable Message message(@NotNull String key) {
        return this.messages.get(key);
    }

    /**
     * Resolves a client locale ({@code fr_FR}, {@code fr-ca}, {@code en_us}) to a language of this manager.
     *
     * @param locale the client locale
     * @return a language code
     */
    @NotNull String resolve(@NotNull String locale) {
        String cached = this.resolved.get(locale);
        if (cached != null) {
            return cached;
        }
        String language = this.match(locale.replace('-', '_').toLowerCase(Locale.ROOT));
        if (this.resolved.size() < MAX_CACHED_LOCALES) {
            this.resolved.put(locale, language);
        }
        return language;
    }

    private @NotNull String match(@NotNull String locale) {
        if (this.languages.contains(locale)) {
            return locale;
        }
        int separator = locale.indexOf('_');
        String base = separator < 0 ? locale : locale.substring(0, separator);
        for (String language : this.languages) {
            if (language.equals(base) || language.startsWith(base + "_")) {
                return language;
            }
        }
        return this.defaultLanguage;
    }
}
