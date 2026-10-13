package fr.robie.messageflow.formatter;

import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import fr.robie.messageflow.api.TextResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * A message sent to many players is formatted once, unless its text depends on the player.
 */
@DisplayName("Shared formatting")
class SharedFormattingTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final AtomicInteger formats = new AtomicInteger();
    private AdventureMessageFormatter<Plugin> formatter;
    private List<Player> players;

    @BeforeEach
    void setUp() {
        this.formatter = new AdventureMessageFormatter<>(mock(Plugin.class)) {
            @Override
            public @NotNull Component format(@Nullable String message, @Nullable Player player, @NotNull Placeholder placeholders) {
                SharedFormattingTest.this.formats.incrementAndGet();
                return super.format(message, player, placeholders);
            }
        };
        TextResolverRegistry registry = new TextResolverRegistry();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            registry.initialize();
        }
        this.formatter.setTextResolverRegistry(registry);

        this.players = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("player" + i);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            this.players.add(player);
        }
    }

    @AfterEach
    void tearDown() {
        GlobalPlaceholderRegistry.getInstance().clear();
    }

    private String received(Player player) {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(captor.capture());
        return PLAIN.serialize(captor.getValue());
    }

    @Test
    @DisplayName("Static placeholders and globals: formatted once for everyone")
    void sharedWhenNotPlayerDependent() {
        GlobalPlaceholderRegistry.getInstance().register("server", "Lobby");
        this.formatter.sendMessage(this.players, "<red>Hi %name% on %server%", false, Placeholder.of("name", "all"));

        assertEquals(1, this.formats.get());
        this.players.forEach(player -> assertEquals("Hi all on Lobby", this.received(player)));
    }

    @Test
    @DisplayName("Player placeholders: formatted for each player with their value")
    void perPlayerWhenPlaceholderIsPlayerSpecific() {
        this.formatter.sendMessage(this.players, "Hi %who%", false, Placeholder.of("who", Player::getName));

        assertEquals(this.players.size(), this.formats.get());
        this.players.forEach(player -> assertEquals("Hi " + player.getName(), this.received(player)));
    }

    @Test
    @DisplayName("Player-specific globals: formatted for each player with their value")
    void perPlayerWhenGlobalIsPlayerSpecific() {
        GlobalPlaceholderRegistry.getInstance().registerPlayer("me", Player::getName);
        this.formatter.sendMessage(this.players, "I am %me%", false, Placeholder.empty());

        assertEquals(this.players.size(), this.formats.get());
        this.players.forEach(player -> assertEquals("I am " + player.getName(), this.received(player)));
    }

    @Test
    @DisplayName("A custom resolver with its own syntax: formatted for each player, even without '%'")
    void perPlayerWithCustomResolver() {
        this.formatter.getTextResolverRegistry().register(new TextResolver() {
            @Override
            public boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
                return player != null && text.contains("{name}");
            }

            @Override
            public @NotNull String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
                return player == null ? text : text.replace("{name}", player.getName());
            }
        });
        this.formatter.sendMessage(this.players, "Hi {name}", false, Placeholder.empty());

        this.players.forEach(player -> assertEquals("Hi " + player.getName(), this.received(player)));
    }

    @Test
    @DisplayName("Modifiers work on local and global placeholders")
    void modifiers() {
        GlobalPlaceholderRegistry.getInstance().register("online", "3");
        GlobalPlaceholderRegistry.getInstance().registerPlayer("me", Player::getName);
        this.formatter.sendMessage(this.players, "<gold>%coins.comma% %online.plural:player:players% %name.upper% %me.upper%",
                false, Placeholder.of("coins", "1500", "name", "steve"));

        this.players.forEach(player -> assertEquals("1,500 players STEVE " + player.getName().toUpperCase(), this.received(player)));
    }
}
