package fr.robie.messageflow.impl;

import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import fr.robie.messageflow.logger.AdventureLogger;
import fr.robie.messageflow.logger.Logger;
import fr.robie.messageflow.model.Message;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MessageManager lifecycle")
class MessageManagerLifecycleTest {
    @TempDir
    Path dataFolder;

    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(true);
        this.bukkit = mockStatic(Bukkit.class);
        this.bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        GlobalPlaceholderRegistry.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        Logger current = Logger.getLogger();
        if (current != null) {
            current.close();
        }
        GlobalPlaceholderRegistry.getInstance().clear();
        this.bukkit.close();
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(false);
    }

    private Plugin plugin(String name) {
        Plugin plugin = mock(Plugin.class);
        PluginMeta meta = mock(PluginMeta.class);
        when(meta.getName()).thenReturn(name);
        when(meta.getVersion()).thenReturn("1.0");
        when(plugin.getPluginMeta()).thenReturn(meta);
        when(plugin.getDataFolder()).thenReturn(this.dataFolder.resolve(name).toFile());
        return plugin;
    }

    @Test
    @DisplayName("A second manager of the same plugin replaces the global logger")
    void secondManagerOfSamePlugin() {
        Plugin plugin = this.plugin("A");
        MessageManager<Plugin, String> first = MessageManager.withSingleFile(plugin, "messages.yml", List.of(new TestMessage()));
        Logger firstLogger = Logger.getLogger();
        assertNotNull(firstLogger);

        MessageManager<Plugin, String> second = assertDoesNotThrow(() -> MessageManager.withSingleFile(plugin, "messages.yml", List.of(new TestMessage())));
        assertNotSame(firstLogger, Logger.getLogger());

        first.close(); // already replaced: must not drop the second manager's logger
        assertNotNull(Logger.getLogger());
        second.close();
        assertNull(Logger.getLogger());
    }

    @Test
    @DisplayName("A logger of another plugin still throws (library not relocated)")
    void loggerOfAnotherPlugin() {
        new AdventureLogger("", new AdventureMessageFormatter<>(this.plugin("A")));
        Logger owner = Logger.getLogger();
        AdventureMessageFormatter<Plugin> other = new AdventureMessageFormatter<>(this.plugin("B"));
        assertThrows(IllegalStateException.class, () -> new AdventureLogger("", other));
        assertSame(owner, Logger.getLogger());
    }

    @Test
    @DisplayName("reload() keeps the global placeholders")
    void reloadKeepsGlobalPlaceholders() {
        GlobalPlaceholderRegistry.getInstance().register("server", "Test");
        MessageManager<Plugin, String> manager = MessageManager.withSingleFile(this.plugin("A"), "messages.yml", List.of(new TestMessage()));
        manager.reload();
        manager.reload();
        assertTrue(GlobalPlaceholderRegistry.getInstance().exists("server"));
        assertTrue(this.dataFolder.resolve("A").resolve("messages.yml").toFile().exists());
    }

    private static final class TestMessage implements Message {
        private final List<? extends MessageTypeAdapter> defaults = List.of(Message.chat("Hello %server%"));
        private List<? extends MessageTypeAdapter> loaded = this.defaults;

        @Override
        public @NotNull String key() {
            return "hello";
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
