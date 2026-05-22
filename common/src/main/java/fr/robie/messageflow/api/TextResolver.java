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
