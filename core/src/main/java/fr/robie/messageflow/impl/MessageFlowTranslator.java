package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.MessageLocalizer;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.Message;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Renders {@code Component.translatable(key, args...)} from a manager's messages, in the language of whoever
 * receives it (registered in Adventure's {@code GlobalTranslator}). Only keys starting with the prefix are
 * answered; the others go on to other translators and then to the client. Argument {@code i} fills placeholder
 * {@code %i%}.
 */
final class MessageFlowTranslator implements Translator {
    private final Key name;
    private final String keyPrefix;
    private final Function<String, @Nullable Message> messages;
    private final AdventureMessageFormatter<?> formatter;

    MessageFlowTranslator(@NotNull Key name, @NotNull String keyPrefix, @NotNull Function<String, @Nullable Message> messages,
                          @NotNull AdventureMessageFormatter<?> formatter) {
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.messages = messages;
        this.formatter = formatter;
    }

    /**
     * Creates a translator and adds it to Adventure's {@code GlobalTranslator}. Kept here, not in the manager, so
     * the manager never names Adventure types (it still loads where Adventure is missing).
     */
    static @NotNull MessageFlowTranslator register(@NotNull String pluginName, @NotNull String keyPrefix,
                                                   @NotNull Function<String, @Nullable Message> messages,
                                                   @NotNull AdventureMessageFormatter<?> formatter) {
        String value = pluginName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        MessageFlowTranslator translator = new MessageFlowTranslator(Key.key("messageflow", value), keyPrefix, messages, formatter);
        GlobalTranslator.translator().addSource(translator);
        return translator;
    }

    /**
     * Removes this translator from Adventure's {@code GlobalTranslator}.
     */
    void unregister() {
        GlobalTranslator.translator().removeSource(this);
    }

    @Override
    public @NotNull Key name() {
        return this.name;
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        return null; // components only: see translate(TranslatableComponent, Locale)
    }

    /**
     * Adventure 5 asks this before rendering; without it, its default renders the whole message to answer.
     */
    public boolean canTranslate(@NotNull String key, @NotNull Locale locale) {
        return key.startsWith(this.keyPrefix) && this.messages.apply(key) != null;
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        String key = component.key();
        if (!key.startsWith(this.keyPrefix)) {
            return null;
        }
        Message message = this.messages.apply(key);
        if (message == null) {
            return null;
        }

        MessageLocalizer localizer = this.formatter.getLocalizer();
        List<? extends MessageTypeAdapter> adapters = localizer != null
                ? localizer.adapters(message, localizer.language(locale))
                : message.loaded().isEmpty() ? message.defaults() : message.loaded();

        Placeholder.Builder arguments = Placeholder.builder();
        List<TranslationArgument> args = component.arguments();
        for (int i = 0; i < args.size(); i++) {
            Object value = args.get(i).value();
            arguments.register(String.valueOf(i), value instanceof Component argument ? argument : Component.text(String.valueOf(value)));
        }

        List<Component> lines = this.formatter.formatLines(adapters, null, arguments.build());
        if (lines.isEmpty()) {
            return null;
        }
        // Adventure 4 returns this as is: keep the translatable's own style and children, as the original had them.
        Component result = lines.size() == 1 ? lines.getFirst() : Component.join(JoinConfiguration.newlines(), lines);
        if (component.children().isEmpty()) {
            return result.applyFallbackStyle(component.style());
        }
        return Component.text().style(component.style()).append(result).append(component.children()).build();
    }
}
