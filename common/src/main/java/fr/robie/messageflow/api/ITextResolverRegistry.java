package fr.robie.messageflow.api;

import fr.robie.messageflow.formatter.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registry for managing and applying {@link TextResolver}s.
 * <p>
 * This registry allows for dynamic expansion of the message resolution process,
 * enabling integration with external placeholder systems (like PlaceholderAPI)
 * or custom logic.
 */
public interface ITextResolverRegistry {
    /**
     * Initializes the registry, typically by registering default resolvers.
     */
    void initialize();

    /**
     * Registers a new text resolver to the registry.
     *
     * @param resolver the resolver to register
     */
    void register(@NotNull TextResolver resolver);

    /**
     * Resolves all registered resolvers on the given text.
     *
     * @param text         the text to resolve
     * @param player       the player context, or null
     * @param placeholders optional placeholders for the resolvers
     * @return the fully resolved text
     */
    @NotNull String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders);

    /**
     * Checks if any registered resolver would change the given text.
     *
     * @param text         the text to check
     * @param player       the player context, or null
     * @param placeholders current placeholders
     * @return true if resolving may change the text
     */
    default boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        return this.hasResolvers();
    }

    /**
     * Checks if resolving the text gives a different result for each player.
     *
     * @param text         the text to check
     * @param placeholders current placeholders
     * @return true if the text must be formatted for each player
     * @see TextResolver#isPlayerDependent(String, Placeholder)
     */
    default boolean isPlayerDependent(@NotNull String text, @NotNull Placeholder placeholders) {
        return this.hasResolvers();
    }

    /**
     * Checks if the registry has any registered resolvers.
     *
     * @return true if resolvers are present, false otherwise
     */
    boolean hasResolvers();
}
