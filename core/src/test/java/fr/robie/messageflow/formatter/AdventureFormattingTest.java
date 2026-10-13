package fr.robie.messageflow.formatter;

import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import fr.robie.messageflow.model.TitleMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Adventure formatting")
class AdventureFormattingTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private AdventureMessageFormatter<Plugin> formatter;

    @BeforeEach
    void setUp() {
        this.formatter = new AdventureMessageFormatter<>(mock(Plugin.class));
        TextResolverRegistry registry = new TextResolverRegistry();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            registry.initialize();
        }
        this.formatter.setTextResolverRegistry(registry);
    }

    @AfterEach
    void tearDown() {
        GlobalPlaceholderRegistry.getInstance().clear();
    }

    @Test
    @DisplayName("Inside a tag, a value can't be read again as a global placeholder or a color")
    void insideTagNotResolvedAgain() {
        GlobalPlaceholderRegistry.getInstance().register("server", "Lobby");
        Component result = this.formatter.format("<click:run_command:'/msg %target%'>go</click>", null,
                Placeholder.of("target", "%server%&a§b"));
        ClickEvent click = flatten(result).stream().map(Component::clickEvent).filter(e -> e != null).findFirst().orElseThrow();
        assertEquals("/msg serverab", click.value());
    }

    @Test
    @DisplayName("Marker characters in resolver output are removed")
    void markersFromResolversRemoved() {
        GlobalPlaceholderRegistry.getInstance().register("nick", "﷐0﷑");
        Component result = this.formatter.format("%nick% %v%", null, Placeholder.of("v", "X"));
        assertEquals("0 X", PLAIN.serialize(result));
    }

    @Test
    @DisplayName("Texts changed by resolvers don't fill the cache of the plugin's messages")
    void changingTextsKeptApart() throws ReflectiveOperationException {
        AtomicInteger tick = new AtomicInteger();
        GlobalPlaceholderRegistry.getInstance().register("tick", () -> String.valueOf(tick.incrementAndGet()));
        for (int i = 0; i < 1000; i++) {
            this.formatter.format("At %tick%: %name%", null, Placeholder.of("name", "Steve"));
        }
        assertEquals("Hello Steve", PLAIN.serialize(this.formatter.format("Hello %name%", null, Placeholder.of("name", "Steve"))));

        Field field = AdventureMessageFormatter.class.getDeclaredField("templates");
        field.setAccessible(true);
        assertEquals(1, ((Cache<?, ?>) field.get(this.formatter)).size(), "only the plugin's own message");
    }

    private static boolean hasClickEvent(Component component) {
        if (component.clickEvent() != null) {
            return true;
        }
        return component.children().stream().anyMatch(AdventureFormattingTest::hasClickEvent);
    }

    private static List<Component> flatten(Component component) {
        List<Component> all = new ArrayList<>();
        all.add(component);
        component.children().forEach(child -> all.addAll(flatten(child)));
        return all;
    }

    @Test
    @DisplayName("Tags in a placeholder value stay text")
    void valueCannotInjectTags() {
        String evil = "<click:run_command:'/op me'>click</click>&4red";
        Component result = this.formatter.format("Hello %name%!", null, Placeholder.of("name", evil));
        assertEquals("Hello " + evil + "!", PLAIN.serialize(result));
        assertFalse(hasClickEvent(result));
    }

    @Test
    @DisplayName("The message keeps its own formatting around a placeholder")
    void messageFormattingStillWorks() {
        Component result = this.formatter.format("&aHello <bold>%name%</bold>", null, Placeholder.of("name", "Steve"));
        assertEquals("Hello Steve", PLAIN.serialize(result));
        assertTrue(flatten(result).stream().anyMatch(c -> NamedTextColor.GREEN.equals(c.color())));
    }

    @Test
    @DisplayName("A player placeholder gets the viewer")
    void playerPlaceholderGetsViewer() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");
        Component result = this.formatter.format("Hi %who%", player, Placeholder.of("who", Player::getName));
        assertEquals("Hi Alex", PLAIN.serialize(result));
    }

    @Test
    @DisplayName("A component placeholder keeps its style")
    void componentPlaceholder() {
        Component value = Component.text("Furnace", NamedTextColor.GOLD);
        Component result = this.formatter.format("Machine: %machine%", null, Placeholder.of("machine", value));
        assertEquals("Machine: Furnace", PLAIN.serialize(result));
        assertTrue(flatten(result).stream().anyMatch(c -> NamedTextColor.GOLD.equals(c.color())));
    }

    @Test
    @DisplayName("Inside a tag, a placeholder is plain text that cannot close the tag")
    void placeholderInsideTag() {
        Component result = this.formatter.format("<click:run_command:'/tp %name%'>go</click>", null,
                Placeholder.of("name", "Steve'><red>x"));
        ClickEvent click = flatten(result).stream().map(Component::clickEvent).filter(e -> e != null).findFirst().orElseThrow();
        assertEquals("/tp Steveredx", click.value()); // the quote and brackets are dropped, so no tag is closed
        assertEquals("go", PLAIN.serialize(result));
    }

    @Test
    @DisplayName("Unknown placeholders and repeated keys")
    void unknownAndRepeated() {
        Component result = this.formatter.format("%a% %b% %a%", null, Placeholder.of("a", "1"));
        assertEquals("1 %b% 1", PLAIN.serialize(result));
    }

    @Test
    @DisplayName("formatLines returns the text lines of a message; formatMessage joins them")
    void messageToComponents() {
        Message message = new TestMessage(List.of(
                new SimpleMessage(MessageType.TCHAT, List.of("Line %n%", "Second")),
                new TitleMessage("Title", "Sub", 1, 1, 1),
                new SimpleMessage(MessageType.ACTION_BAR, List.of("Bar"))));

        List<Component> lines = this.formatter.formatLines(message, Placeholder.of("n", "1"));
        assertEquals(List.of("Line 1", "Second", "Bar"), lines.stream().map(PLAIN::serialize).toList());
        assertEquals("Line 1\nSecond\nBar", PLAIN.serialize(this.formatter.formatMessage(message, Placeholder.of("n", "1"))));
        assertEquals("Line %n%\nSecond\nBar", PLAIN.serialize(this.formatter.formatMessage(message)));
    }

    private record TestMessage(List<? extends MessageTypeAdapter> defaults) implements Message {
        @Override
        public @NotNull String key() {
            return "test";
        }

        @Override
        public @NotNull List<? extends MessageTypeAdapter> loaded() {
            return this.defaults;
        }

        @Override
        public void setLoaded(@NotNull List<? extends MessageTypeAdapter> loaded) {
        }
    }
}
