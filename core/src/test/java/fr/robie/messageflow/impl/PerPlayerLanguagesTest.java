package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.configuration.lang.NormalLanguageConfiguration;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.model.Message;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Per-player languages")
class PerPlayerLanguagesTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @TempDir
    Path dataFolder;

    private MockedStatic<Bukkit> bukkit;
    private MessageManager<Plugin, String> manager;
    private final TestMessage hello = new TestMessage("greeting.hello", "Hello %0%");
    private final TestMessage bye = new TestMessage("greeting.bye", "Bye");

    @BeforeEach
    void setUp() throws IOException {
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(true);
        this.bukkit = mockStatic(Bukkit.class);
        this.bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

        Path lang = Files.createDirectories(this.dataFolder.resolve("lang"));
        Files.writeString(lang.resolve("en_us.yml"), "greeting:\n  hello: \"Hello %0%\"\n", StandardCharsets.UTF_8);
        Files.writeString(lang.resolve("fr_fr.yml"), "greeting:\n  hello: \"Bonjour %0%\"\n", StandardCharsets.UTF_8);

        Plugin plugin = mock(Plugin.class);
        PluginMeta meta = mock(PluginMeta.class);
        when(meta.getName()).thenReturn("Test");
        when(meta.getVersion()).thenReturn("1.0");
        when(plugin.getPluginMeta()).thenReturn(meta);
        when(plugin.getName()).thenReturn("Test");
        when(plugin.getDataFolder()).thenReturn(this.dataFolder.toFile());

        NormalLanguageConfiguration languages = new NormalLanguageConfiguration("en_us");
        languages.addLanguage("en_us", "lang/en_us.yml");
        languages.addLanguage("fr_fr", "lang/fr_fr.yml");
        languages.addLanguage("de_de", "lang/de_de.yml"); // no file and nothing bundled
        this.manager = new MessageManager<>(plugin, languages, List.of(this.hello, this.bye));
        this.manager.setPerPlayerLanguages(true);
        this.manager.reload();
    }

    @AfterEach
    void tearDown() {
        this.manager.close();
        this.bukkit.close();
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(false);
    }

    private static Player player(String locale) {
        Player player = mock(Player.class);
        when(player.locale()).thenReturn(Locale.forLanguageTag(locale));
        return player;
    }

    private String text(Message message, Player viewer) {
        return PLAIN.serialize(this.manager.adventure().formatMessage(message, viewer, Placeholder.of("0", "Steve")));
    }

    @Test
    @DisplayName("Missing keys go to the default language only; no file is made from the defaults for another one")
    void filesOnDisk() {
        YamlConfiguration english = YamlConfiguration.loadConfiguration(this.dataFolder.resolve("lang/en_us.yml").toFile());
        YamlConfiguration french = YamlConfiguration.loadConfiguration(this.dataFolder.resolve("lang/fr_fr.yml").toFile());
        assertEquals("Bye", english.getString("greeting.bye"));
        assertFalse(french.contains("greeting.bye"));
        assertFalse(Files.exists(this.dataFolder.resolve("lang/de_de.yml")));
    }

    @Test
    @DisplayName("A player reads their client's language, with the fallback chain")
    void fallbackChain() {
        assertEquals("Bonjour Steve", this.text(this.hello, player("fr-FR")));
        assertEquals("Bonjour Steve", this.text(this.hello, player("fr-CA")), "same language, other country");
        assertEquals("Hello Steve", this.text(this.hello, player("de-DE")), "no file for de_de");
        assertEquals("Hello Steve", this.text(this.hello, player("ja-JP")), "unknown language");
        assertEquals("Hello Steve", this.text(this.hello, null), "no player: default language");
        assertEquals("Bye", this.text(this.bye, player("fr-FR")), "missing from fr_fr: default language");
    }

    @Test
    @DisplayName("One send reaches each recipient in their own language")
    void sendGroupsByLanguage() {
        Player english = player("en-US");
        Player french = player("fr-FR");
        this.manager.formatter().sendMessage(this.hello, List.of(english, french), false, Placeholder.of("0", "Steve"));

        ArgumentCaptor<Component> toEnglish = ArgumentCaptor.forClass(Component.class);
        ArgumentCaptor<Component> toFrench = ArgumentCaptor.forClass(Component.class);
        verify(english).sendMessage(toEnglish.capture());
        verify(french).sendMessage(toFrench.capture());
        assertEquals(List.of("Hello Steve"), toEnglish.getAllValues().stream().map(PLAIN::serialize).toList());
        assertEquals(List.of("Bonjour Steve"), toFrench.getAllValues().stream().map(PLAIN::serialize).toList());
    }

    @Test
    @DisplayName("Translatable components render server-side through the translator")
    void translatorBridge() {
        this.manager.registerTranslator("greeting.");
        Component translatable = Component.translatable("greeting.hello", NamedTextColor.GOLD, Component.text("Steve"))
                .append(Component.text("!"));

        Component french = GlobalTranslator.render(translatable, Locale.FRANCE);
        assertEquals("Bonjour Steve!", PLAIN.serialize(french));
        assertEquals(NamedTextColor.GOLD, french.color());
        assertEquals("Hello Steve!", PLAIN.serialize(GlobalTranslator.render(translatable, Locale.US)));

        Component other = Component.translatable("other.key");
        assertSame(other, GlobalTranslator.render(other, Locale.FRANCE), "keys without the prefix are left alone");

        this.manager.close();
        Component closed = GlobalTranslator.render(translatable, Locale.FRANCE);
        assertInstanceOf(net.kyori.adventure.text.TranslatableComponent.class, closed, "close() removes the translator");
    }

    @Test
    @DisplayName("setActiveLanguage changes the default language")
    void activeLanguageIsDefault() {
        this.manager.setActiveLanguage("fr_fr");
        assertEquals("Bonjour Steve", this.text(this.hello, null));
        assertEquals("Hello Steve", this.text(this.hello, player("en-US")));
    }

    private static final class TestMessage implements Message {
        private final String key;
        private final List<? extends MessageTypeAdapter> defaults;
        private volatile List<? extends MessageTypeAdapter> loaded;

        TestMessage(String key, String text) {
            this.key = key;
            this.defaults = List.of(Message.chat(text));
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
