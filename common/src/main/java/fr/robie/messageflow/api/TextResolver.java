package fr.robie.messageflow.api;

import fr.robie.messageflow.formatter.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for components that can resolve custom placeholders or hooks within text.
 * <p>
 * Resolvers are applied during the message formatting process to transform raw text
 * into its final form, often based on a player context.
 */
public interface TextResolver {

    /**
     * Checks if this resolver can handle any part of the given text.
     *
     * @param text         the text to check
     * @param player       the player context, or null
     * @param placeholders current placeholders
     * @return true if this resolver can apply changes, false otherwise
     */
    boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders);

    /**
     * Checks if resolving the text gives a different result for each player. When it does not, a message sent to
     * many players is formatted once and shared.
     *
     * @param text         the text to check
     * @param placeholders current placeholders
     * @return true if the result depends on the player. By default always true: a resolver may use any syntax
     * ({@code {player}}, {@code <papi:...>}) or only apply with a player, so it is formatted for each player
     * unless it overrides this.
     */
    default boolean isPlayerDependent(@NotNull String text, @NotNull Placeholder placeholders) {
        return true;
    }

    /**
     * Resolves placeholders or hooks within the given text.
     *
     * @param text         the text to resolve
     * @param player       the player context, or null
     * @param placeholders current placeholders
     * @return the resolved text
     */
    @NotNull
    String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders);

}
