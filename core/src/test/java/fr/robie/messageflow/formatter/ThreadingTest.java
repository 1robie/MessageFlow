package fr.robie.messageflow.formatter;

import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.MessageTypeAdapter;
import fr.robie.messageflow.model.AdventureBossBarMessage;
import fr.robie.messageflow.model.Message;
import fr.robie.messageflow.model.MessageType;
import fr.robie.messageflow.model.SimpleMessage;
import fr.robie.messageflow.model.SoundMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sounds and boss bars run on the main thread; a message can be sent from an async thread.
 */
@DisplayName("Threading")
class ThreadingTest {
    private MockedStatic<Bukkit> bukkit;
    private BukkitScheduler scheduler;
    private Plugin plugin;
    private AdventureMessageFormatter<Plugin> formatter;
    private Player player;
    private boolean mainThread;

    @BeforeEach
    void setUp() {
        this.bukkit = mockStatic(Bukkit.class);
        this.bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        this.bukkit.when(Bukkit::isPrimaryThread).thenAnswer(invocation -> this.mainThread);

        this.scheduler = mock(BukkitScheduler.class);
        Server server = mock(Server.class);
        when(server.getScheduler()).thenReturn(this.scheduler);
        this.plugin = mock(Plugin.class);
        when(this.plugin.getServer()).thenReturn(server);

        this.formatter = new AdventureMessageFormatter<>(this.plugin);
        TextResolverRegistry registry = new TextResolverRegistry();
        registry.initialize();
        this.formatter.setTextResolverRegistry(registry);
        this.player = mock(Player.class);
    }

    @AfterEach
    void tearDown() {
        this.bukkit.close();
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

    /**
     * A sound message ({@link Sound} needs a server registry, so the sound itself is null).
     */
    private static SoundMessage sound() {
        SoundMessage sound = mock(SoundMessage.class);
        when(sound.category()).thenReturn(SoundCategory.MASTER);
        when(sound.volume()).thenReturn(1f);
        when(sound.pitch()).thenReturn(1f);
        return sound;
    }

    private Runnable scheduled(boolean later) {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        if (later) {
            verify(this.scheduler).runTaskLater(eq(this.plugin), task.capture(), anyLong());
        } else {
            verify(this.scheduler).runTask(eq(this.plugin), task.capture());
        }
        return task.getValue();
    }

    @Test
    @DisplayName("On the main thread a sound plays right away")
    void soundOnMainThread() {
        this.mainThread = true;
        this.formatter.playSound(this.player, sound());

        verify(this.player).playSound(eq(this.player), isNull(Sound.class), eq(SoundCategory.MASTER), eq(1f), eq(1f));
        verifyNoInteractions(this.scheduler);
    }

    @Test
    @DisplayName("Off the main thread a sound is handed to the main thread")
    void soundOffMainThread() {
        this.mainThread = false;
        this.formatter.playSound(this.player, sound());

        verify(this.player, never()).playSound(any(Player.class), isNull(Sound.class), any(SoundCategory.class), anyFloat(), anyFloat());
        this.scheduled(false).run();
        verify(this.player).playSound(eq(this.player), isNull(Sound.class), eq(SoundCategory.MASTER), eq(1f), eq(1f));
    }

    @Test
    @DisplayName("A boss bar is hidden after its duration")
    void bossBarHidden() {
        this.mainThread = true;
        Message message = new TestMessage(List.of(new AdventureBossBarMessage("Bar", BossBar.Color.RED,
                BossBar.Overlay.PROGRESS, Set.of(), 40L, 1f)));
        this.formatter.sendMessage(message, this.player);

        verify(this.player).showBossBar(any(BossBar.class));
        verify(this.scheduler).runTaskLater(eq(this.plugin), any(Runnable.class), eq(40L));
        this.scheduled(true).run();
        verify(this.player).hideBossBar(any(BossBar.class));
    }

    @Test
    @DisplayName("sendMessageAsync sends from an async task and completes")
    void sendAsync() throws Exception {
        this.mainThread = true;
        when(this.scheduler.runTaskAsynchronously(eq(this.plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            Thread thread = new Thread(() -> {
                this.mainThread = false;
                task.run();
            });
            thread.start();
            return null;
        });
        Message message = new TestMessage(List.of(new SimpleMessage(MessageType.TCHAT, List.of("Hi %name%"))));

        this.formatter.sendMessageAsync(message, this.player, Placeholder.of("name", "Steve")).get(5, TimeUnit.SECONDS);

        ArgumentCaptor<Component> sent = ArgumentCaptor.forClass(Component.class);
        verify(this.player).sendMessage(sent.capture());
        assertEquals("Hi Steve", PlainTextComponentSerializer.plainText().serialize(sent.getValue()));
    }

    @Test
    @DisplayName("sendMessageAsync completes exceptionally when sending fails")
    void sendAsyncFailure() {
        when(this.scheduler.runTaskAsynchronously(eq(this.plugin), any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        });
        doThrow(new IllegalStateException("boom")).when(this.player).sendMessage(any(Component.class));
        Message message = new TestMessage(List.of(new SimpleMessage(MessageType.TCHAT, List.of("Hi"))));

        assertTrue(this.formatter.sendMessageAsync(message, this.player).isCompletedExceptionally());
    }
}
