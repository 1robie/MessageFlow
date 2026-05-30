package fr.robie.messageflow.api;

import fr.robie.messageflow.formatter.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A fluent builder for creating and sending messages on-the-fly without pre-defining them in an Enum.
 * <p>
 * This builder allows combining multiple message types (chat, action bar, title, sound)
 * and placeholders in a single delivery.
 */
public interface IMessageBuilder {

    /**
     * Adds one or more chat message lines.
     *
     * @param lines the chat message lines to add
     * @return this builder
     */
    @NotNull IMessageBuilder chat(@NotNull String... lines);

    /**
     * Adds an action bar message.
     *
     * @param line the action bar message line
     * @return this builder
     */
    @NotNull IMessageBuilder actionBar(@NotNull String line);

    /**
     * Adds a title message with specified timings.
     *
     * @param title    the main title text
     * @param subtitle the subtitle text
     * @param fadeIn   fade-in time in ticks
     * @param stay     stay time in ticks
     * @param fadeOut  fade-out time in ticks
     * @return this builder
     */
    @NotNull IMessageBuilder title(@NotNull String title, @NotNull String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * Adds a title message with default timings (10, 70, 20).
     *
     * @param title    the main title text
     * @param subtitle the subtitle text
     * @return this builder
     */
    @NotNull IMessageBuilder title(@NotNull String title, @NotNull String subtitle);

    /**
     * Adds a sound to be played.
     *
     * @param sound  the sound name
     * @param volume the volume
     * @param pitch  the pitch
     * @return this builder
     */
    @NotNull IMessageBuilder sound(@NotNull String sound, float volume, float pitch);

    /**
     * Adds a sound to be played with default volume and pitch (1.0).
     *
     * @param sound the sound name
     * @return this builder
     */
    @NotNull IMessageBuilder sound(@NotNull String sound);

    /**
     * Adds a static placeholder.
     *
     * @param key   the placeholder key (e.g., "reason")
     * @param value the static value
     * @return this builder
     */
    @NotNull IMessageBuilder placeholder(@NotNull String key, @NotNull String value);

    /**
     * Adds a dynamic placeholder evaluated at resolution time.
     *
     * @param key   the placeholder key
     * @param value the supplier for the value
     * @return this builder
     */
    @NotNull IMessageBuilder placeholder(@NotNull String key, @NotNull Supplier<String> value);

    /**
     * Adds a player-specific placeholder evaluated per player.
     *
     * @param key   the placeholder key
     * @param value the function for the value based on a player
     * @return this builder
     */
    @NotNull IMessageBuilder placeholder(@NotNull String key, @NotNull Function<Player, String> value);

    /**
     * Adds multiple placeholders from an existing Placeholder instance.
     *
     * @param placeholders the placeholders to add
     * @return this builder
     */
    @NotNull IMessageBuilder placeholders(@NotNull Placeholder placeholders);

    /**
     * Sets whether to prepend the plugin prefix to chat messages.
     * Default is true.
     *
     * @param prefix true to use prefix, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder prefix(boolean prefix);

    /**
     * Sets whether to broadcast the message to all online players.
     *
     * @param broadcast true to broadcast, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder broadcast(boolean broadcast);

    /**
     * Sets whether to send the message to the console.
     *
     * @param sendToConsole true to send to console, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder sendToConsole(boolean sendToConsole);

    /**
     * Sets whether to exclude the recipients passed to {@link #send(Collection)} from the broadcast.
     *
     * @param exclude true to exclude recipients, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder excludeSenders(boolean exclude);

    /**
     * Sets whether the most recently added message component should be broadcast to all players.
     *
     * @param broadcast true to broadcast, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder withBroadcast(boolean broadcast);

    /**
     * Sets whether the most recently added message component should be sent to the console.
     *
     * @param sendToConsole true to send to console, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder withSendToConsole(boolean sendToConsole);

    /**
     * Sets whether the initial recipients should be excluded from the broadcast for the most
     * recently added message component.
     *
     * @param exclude true to exclude recipients, false otherwise
     * @return this builder
     */
    @NotNull IMessageBuilder withExcludeSenders(boolean exclude);

    /**
     * Sends the constructed message to a single command sender.
     *
     * @param sender the recipient
     */
    void send(@NotNull CommandSender sender);

    /**
     * Sends the constructed message to a collection of command senders.
     *
     * @param senders the recipients
     */
    void send(@NotNull Collection<? extends CommandSender> senders);

    /**
     * Broadcasts the constructed message to all online players.
     */
    void broadcast();
}
