package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.configuration.lang.NormalLanguageConfiguration;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import fr.robie.messageflow.model.TitleMessage;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Language files")
class YamlParsingTest {
    private static final String FILE = """
            plain: "Hello"
            lines:
              - "Line 1"
              - "Line 2"
            title:
              type: TITLE
              title: "Big"
              subtitle: "small"
              fade-in: 5
              stay: 40
              fade-out: 6
            bar:
              type: ACTION_BAR
              message: "Bar text"
            multi:
              - type: TCHAT
                message: "Chat"
              - type: ACTION_BAR
                message: "Bar"
                broadcast: true
            wrapped:
              send-to-console: true
              messages:
                - "One"
                - "Two"
            unknown-type:
              type: NOPE
              message: "x"
            nested:
              key: "Nested value"
            """;

    @TempDir
    Path dataFolder;

    private MockedStatic<Bukkit> bukkit;
    private Plugin plugin;

    private final TestMessage plain = new TestMessage("plain");
    private final TestMessage lines = new TestMessage("lines");
    private final TestMessage title = new TestMessage("title");
    private final TestMessage bar = new TestMessage("bar");
    private final TestMessage multi = new TestMessage("multi");
    private final TestMessage wrapped = new TestMessage("wrapped");
    private final TestMessage unknownType = new TestMessage("unknown-type");
    private final TestMessage nested = new TestMessage("nested.key");
    private final TestMessage missing = new TestMessage("missing");
    private final List<TestMessage> all = List.of(this.plain, this.lines, this.title, this.bar, this.multi, this.wrapped,
            this.unknownType, this.nested, this.missing);

    @BeforeEach
    void setUp() throws IOException {
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(true);
        this.bukkit = mockStatic(Bukkit.class);
        this.bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        Files.writeString(this.dataFolder.resolve("messages.yml"), FILE, StandardCharsets.UTF_8);

        this.plugin = mock(Plugin.class);
        PluginMeta meta = mock(PluginMeta.class);
        when(meta.getName()).thenReturn("Test");
        when(meta.getVersion()).thenReturn("1.0");
        when(this.plugin.getPluginMeta()).thenReturn(meta);
        when(this.plugin.getName()).thenReturn("Test");
        when(this.plugin.getDataFolder()).thenReturn(this.dataFolder.toFile());
    }

    @AfterEach
    void tearDown() {
        this.bukkit.close();
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(false);
    }

    private MessageManager<Plugin, String> load() {
        MessageManager<Plugin, String> manager = MessageManager.withSingleFile(this.plugin, "messages.yml", this.all);
        manager.reload();
        return manager;
    }

    private static List<String> text(MessageTypeAdapter adapter) {
        return ((SimpleMessage) adapter).messages();
    }

    @Test
    @DisplayName("Every value shape parses into the right adapters")
    void valueShapes() {
        MessageManager<Plugin, String> manager = this.load();
        try {
            assertEquals(List.of("Hello"), text(this.plain.loaded().getFirst()));
            assertEquals(MessageType.TCHAT, this.plain.loaded().getFirst().messageType());

            assertEquals(1, this.lines.loaded().size(), "a list of strings is one chat message");
            assertEquals(List.of("Line 1", "Line 2"), text(this.lines.loaded().getFirst()));

            TitleMessage title = (TitleMessage) this.title.loaded().getFirst();
            assertEquals(List.of("Big", "small", 5, 40, 6), List.of(title.title(), title.subtitle(), title.fadeIn(), title.stay(), title.fadeOut()));

            assertEquals(MessageType.ACTION_BAR, this.bar.loaded().getFirst().messageType());
            assertEquals(List.of("Bar text"), text(this.bar.loaded().getFirst()));

            assertEquals(List.of(MessageType.TCHAT, MessageType.ACTION_BAR), this.multi.loaded().stream().map(MessageTypeAdapter::messageType).toList());
            assertFalse(this.multi.loaded().get(0).broadcast());
            assertTrue(this.multi.loaded().get(1).broadcast(), "settings apply per entry");

            assertEquals(List.of("One", "Two"), text(this.wrapped.loaded().getFirst()));
            assertTrue(this.wrapped.loaded().getFirst().sendToConsole(), "settings of a wrapped list apply to its messages");

            assertEquals(List.of("Nested value"), text(this.nested.loaded().getFirst()), "dotted keys are YAML paths");
        } finally {
            manager.close();
        }
    }

    @Test
    @DisplayName("An unknown type is skipped, and the formatter then uses the defaults")
    void unknownType() {
        MessageManager<Plugin, String> manager = this.load();
        try {
            assertTrue(this.unknownType.loaded().isEmpty());
            assertEquals("default unknown-type",
                    PlainTextComponentSerializer.plainText().serialize(manager.adventure().formatMessage(this.unknownType)));
        } finally {
            manager.close();
        }
    }

    @Test
    @DisplayName("A missing key is written with its default and loaded")
    void missingKeyAdded() throws IOException {
        MessageManager<Plugin, String> manager = this.load();
        try {
            assertEquals(List.of("default missing"), text(this.missing.loaded().getFirst()));
            assertTrue(Files.readString(this.dataFolder.resolve("messages.yml")).contains("default missing"));
        } finally {
            manager.close();
        }
    }

    @Test
    @DisplayName("reload() picks up edited files, per-player languages included")
    void reloadSwapsText() throws IOException {
        Files.writeString(this.dataFolder.resolve("en.yml"), "plain: \"Hello\"\n", StandardCharsets.UTF_8);
        Files.writeString(this.dataFolder.resolve("fr.yml"), "plain: \"Bonjour\"\n", StandardCharsets.UTF_8);
        NormalLanguageConfiguration languages = new NormalLanguageConfiguration("en_us");
        languages.addLanguage("en_us", "en.yml");
        languages.addLanguage("fr_fr", "fr.yml");
        MessageManager<Plugin, String> manager = new MessageManager<>(this.plugin, languages, List.<Message>of(this.plain));
        manager.setPerPlayerLanguages(true);
        manager.reload();
        Player french = mock(Player.class);
        when(french.locale()).thenReturn(Locale.FRANCE);
        try {
            assertEquals("Bonjour", this.plainText(manager, french));

            Files.writeString(this.dataFolder.resolve("en.yml"), "plain: \"Hi\"\n", StandardCharsets.UTF_8);
            Files.writeString(this.dataFolder.resolve("fr.yml"), "plain: \"Salut\"\n", StandardCharsets.UTF_8);
            manager.reload();

            assertEquals("Hi", this.plainText(manager, null));
            assertEquals("Salut", this.plainText(manager, french));
        } finally {
            manager.close();
        }
    }

    private String plainText(MessageManager<Plugin, String> manager, Player viewer) {
        return PlainTextComponentSerializer.plainText().serialize(manager.adventure().formatMessage(this.plain, viewer, Placeholder.empty()));
    }

    private static final class TestMessage implements Message {
        private final String key;
        private final List<? extends MessageTypeAdapter> defaults;
        private volatile List<? extends MessageTypeAdapter> loaded;

        TestMessage(String key) {
            this.key = key;
            this.defaults = List.of(Message.chat("default " + key));
            this.loaded = this.defaults;
        }

        @Override
        public @NotNull String key() {
            return this.key;
        }

        @Override
        public @NotNull List<? extends MessageTypeAdapter> defaults() {
            return this.defaults;
        }

        @Override
        public @NotNull List<? extends MessageTypeAdapter> loaded() {
            return this.loaded;
        }

        @Override
        public void setLoaded(@NotNull List<? extends MessageTypeAdapter> loaded) {
            this.loaded = loaded;
        }
    }
}
