package fr.robie.messageflow.formatter;

import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.PlaceholderValue;
import fr.robie.messageflow.util.PlaceholderScanner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.VirtualComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Messages with placeholders are parsed once and reused: the result is the same as parsing each time.
 */
@DisplayName("Template cache")
class TemplateCacheTest {
    private static final String[] PIECES = {
            "Hello ", "x", " ", "<red>", "</red>", "<bold>", "</bold>", "<italic>", "<reset>", "<newline>",
            "<gradient:red:blue>", "</gradient>", "<rainbow>", "</rainbow>", "<transition:red:blue:0.5>", "</transition>",
            "<click:run_command:'/tp %a%'>", "</click>", "<hover:show_text:'hi %b%'>", "</hover>",
            "<hover:show_text:'<gold>%a%'>", "%a%", "%a%", "%b%", "%c%", "%missing%", "&c", "&l", "#ff00aa",
            "<color:#00ff00>", "</color>", "<mfph_3>", "<insert:'%b%'>", "</insert>", "\\<red>", "50% ",
    };

    private AdventureMessageFormatter<Plugin> formatter;
    private Placeholder placeholders;

    @BeforeEach
    void setUp() {
        this.formatter = new AdventureMessageFormatter<>(mock(Plugin.class));
        TextResolverRegistry registry = new TextResolverRegistry();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            registry.initialize();
        }
        this.formatter.setTextResolverRegistry(registry);
        this.placeholders = Placeholder.builder()
                .register("a", "Steve")
                .register("b", "<red>inject")
                .register("c", Component.text("C", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .build();
    }

    @Test
    @DisplayName("Same result as parsing each time, first and second call")
    void sameAsDirectParse() {
        Random random = new Random(1);
        for (int n = 0; n < 20_000; n++) {
            StringBuilder message = new StringBuilder();
            for (int i = random.nextInt(8); i >= 0; i--) {
                message.append(PIECES[random.nextInt(PIECES.length)]);
            }
            String text = message.toString();
            Component expected = OldFormat.format(text, this.placeholders);
            assertSame(expected, this.formatter.format(text, null, this.placeholders), text);
            assertSame(expected, this.formatter.format(text, null, this.placeholders), "cached: " + text);
        }
    }

    /**
     * Equal components. A virtual component ({@code <gradient>}) holds a renderer without equals: those are
     * compared by their MiniMessage form and plain text.
     */
    private static void assertSame(Component expected, Component actual, String message) {
        if (hasVirtual(expected) || hasVirtual(actual)) {
            assertEquals(MiniMessage.miniMessage().serialize(expected), MiniMessage.miniMessage().serialize(actual), message);
            assertEquals(PlainTextComponentSerializer.plainText().serialize(expected), PlainTextComponentSerializer.plainText().serialize(actual), message);
        } else {
            assertEquals(expected, actual, message);
        }
    }

    private static boolean hasVirtual(Component component) {
        return component instanceof VirtualComponent || component.children().stream().anyMatch(TemplateCacheTest::hasVirtual);
    }

    @Test
    @DisplayName("A cached message takes the new values")
    void newValuesOnCachedMessage() {
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        String message = "<red>Hi %name%, you have %coins% coins";
        for (int i = 0; i < 3; i++) {
            Placeholder values = Placeholder.of("name", "P" + i, "coins", String.valueOf(i * 10));
            assertEquals("Hi P" + i + ", you have " + (i * 10) + " coins", plain.serialize(this.formatter.format(message, null, values)));
        }
    }

    /**
     * The previous implementation: placeholders as tags resolved by MiniMessage on every call.
     */
    private static final class OldFormat {
        private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
                .tags(TagResolver.builder().resolver(StandardTags.defaults()).build())
                .build();
        private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

        static Component format(String message, Placeholder placeholders) {
            TagResolver.Builder tags = TagResolver.builder();
            Map<String, String> tagNames = new HashMap<>();
            int[] state = {0, 0}; // tag depth, scanned
            String text = PlaceholderScanner.replace(message, (key, start) -> {
                state[0] = depth(message, state[1], start, state[0]);
                state[1] = start;
                PlaceholderValue value = placeholders.get(key);
                if (value == null) {
                    return null;
                }
                Component component = value instanceof PlaceholderValue.ComponentValue(Component c) ? c : Component.text(evaluate(value));
                if (state[0] > 0) {
                    return PLAIN.serialize(component).replaceAll("[<>'\"\\\\]", "");
                }
                String tagName = tagNames.get(key);
                if (tagName == null) {
                    tagName = "mfph_" + tagNames.size();
                    tagNames.put(key, tagName);
                    tags.resolver(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(tagName, component));
                }
                return "<" + tagName + ">";
            });
            return MINI_MESSAGE.deserialize(AdventureMessageFormatter.colorMiniMessage(text), tags.build());
        }

        private static String evaluate(PlaceholderValue value) {
            try {
                return value.evaluate(null);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static int depth(String message, int from, int to, int depth) {
            for (int i = from; i < to; i++) {
                char c = message.charAt(i);
                if (c == '\\') {
                    i++;
                } else if (c == '<') {
                    depth++;
                } else if (c == '>' && depth > 0) {
                    depth--;
                }
            }
            return depth;
        }
    }
}
