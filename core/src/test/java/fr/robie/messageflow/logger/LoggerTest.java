package fr.robie.messageflow.logger;

import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.impl.MessageManager;
import fr.robie.messageflow.model.Message;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Logger")
class LoggerTest {
    private final List<LogRecord> records = new ArrayList<>();
    private final Handler capture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            LoggerTest.this.records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private static java.util.logging.Logger julNamed(String name) {
        java.util.logging.Logger jul = java.util.logging.Logger.getLogger(name);
        jul.setUseParentHandlers(false);
        return jul;
    }

    @BeforeEach
    void setUp() {
        Logger current = Logger.getLogger();
        if (current != null) {
            current.close();
        }
    }

    @AfterEach
    void tearDown() {
        julNamed("MessageFlow").removeHandler(this.capture);
        julNamed("LoggerTest").removeHandler(this.capture);
        Logger.setDebug(false);
    }

    @Test
    @DisplayName("Without a manager, static calls go to the MessageFlow JUL logger, tags removed, values kept")
    void staticCallsWithoutManager() {
        julNamed("MessageFlow").addHandler(this.capture);
        Logger.info("[core]", "<red>Loaded %name%", Placeholder.of("name", "<b>Steve"));
        Logger.debug(() -> "hidden"); // debug is off

        assertEquals(1, this.records.size());
        assertEquals(Level.INFO, this.records.getFirst().getLevel());
        assertEquals("[core] Loaded <b>Steve", this.records.getFirst().getMessage());
    }

    @Test
    @DisplayName("Logger.jul attaches the exception to the record")
    void exceptionAttached() {
        java.util.logging.Logger jul = julNamed("LoggerTest");
        jul.addHandler(this.capture);
        IllegalStateException failure = new IllegalStateException("boom");
        Logger.jul(jul).logError("Failed", failure);

        assertEquals(Level.SEVERE, this.records.getFirst().getLevel());
        assertEquals("Failed", this.records.getFirst().getMessage());
        assertSame(failure, this.records.getFirst().getThrown());
    }

    @Test
    @DisplayName("A debug supplier is only called when debug is on")
    void lazyDebug() {
        java.util.logging.Logger jul = julNamed("LoggerTest");
        jul.addHandler(this.capture);
        Logger logger = Logger.jul(jul);
        AtomicBoolean built = new AtomicBoolean();

        logger.logDebug(() -> {
            built.set(true);
            return "x";
        });
        assertFalse(built.get());
        assertTrue(this.records.isEmpty());

        logger.setDebugEnabled(true);
        logger.logDebug("[ticks]", () -> "<took> 3 ms");
        assertEquals("[ticks] <took> 3 ms", this.records.getFirst().getMessage(), "a supplied value is never parsed as tags");
    }

    @Test
    @DisplayName("toJul forwards records: levels, parameters, exceptions; fine is debug")
    void toJulView() {
        java.util.logging.Logger jul = julNamed("LoggerTest");
        jul.addHandler(this.capture);
        java.util.logging.Logger view = Logger.jul(jul).toJul();

        view.log(Level.WARNING, "Bad value {0} in <file>", "x");
        view.fine("hidden");
        RuntimeException failure = new RuntimeException();
        view.log(Level.SEVERE, "Crashed", failure);

        assertEquals(2, this.records.size());
        assertEquals(Level.WARNING, this.records.get(0).getLevel());
        assertEquals("Bad value x in <file>", this.records.get(0).getMessage());
        assertSame(failure, this.records.get(1).getThrown());
    }

    @Test
    @DisplayName("A manager's logger sends an exception to the plugin's JUL logger, as plain text")
    void managerLoggerException() {
        ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            java.util.logging.Logger pluginLogger = julNamed("LoggerTest");
            pluginLogger.addHandler(this.capture);
            Plugin plugin = mock(Plugin.class);
            PluginMeta meta = mock(PluginMeta.class);
            when(meta.getName()).thenReturn("Test");
            when(meta.getVersion()).thenReturn("1.0");
            when(plugin.getPluginMeta()).thenReturn(meta);
            when(plugin.getLogger()).thenReturn(pluginLogger);

            MessageManager<Plugin, String> manager = MessageManager.withSingleFile(plugin, "messages.yml", List.<Message>of());
            IllegalStateException failure = new IllegalStateException();
            Logger.error("[core]", "<red>Failed %what%", failure, Placeholder.of("what", "<b>save"));
            manager.close();

            assertEquals(Level.SEVERE, this.records.getFirst().getLevel());
            assertEquals("[core] Failed <b>save", this.records.getFirst().getMessage());
            assertSame(failure, this.records.getFirst().getThrown());
        } finally {
            ConfigurationManager.Setting.BYPASS_EXTERNAL_CONFIG.setValue(false);
        }
    }
}
