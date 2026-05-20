package fr.robie.messageflow.formatter;

import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import fr.robie.messageflow.api.TextResolver;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;

/**
 * TextResolver implementation that resolves global placeholders with support for
 * dynamic values (suppliers) and player-specific values (functions).
 *
 * <p>This resolver integrates with the global placeholder registry and handles:
 * <ul>
 *   <li>Evaluation of supplier-based placeholders at resolution time</li>
 *   <li>Player-specific function-based placeholders with optional per-player caching</li>
 *   <li>Error handling with color-coded fallback placeholders</li>
 * </ul>
 *
 * <p>Resolution order in the message formatting pipeline:
 * <ol>
 *   <li>Local placeholders (highest priority)</li>
 *   <li>Global placeholders (this resolver)</li>
 *   <li>External resolvers (e.g., PlaceholderAPI)</li>
 * </ol>
 */
public final class FunctionalPlaceholderResolver implements TextResolver {
    private final GlobalPlaceholderRegistry globalPlaceholderRegistry = GlobalPlaceholderRegistry.getInstance();

    private static final String ERROR_FORMAT = "§c%s§r";

    @Override
    public boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        return this.globalPlaceholderRegistry.hasAnyInText(text);
    }

    @Override
    public @NotNull String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        if (this.globalPlaceholderRegistry.isEmpty()) {
            return text;
        }

        Matcher matcher = Placeholder.PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group();
            String key = placeholder.substring(1, placeholder.length() - 1);

            if (this.globalPlaceholderRegistry.exists(key)) {
                String replacement = this.evaluateGlobalPlaceholder(key, player);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Evaluates a global placeholder by key, handling player context and errors.
     *
     * @param key    the placeholder key
     * @param player the player context (may be null)
     * @return the evaluated value, or error-formatted key if evaluation fails
     */
    private @NotNull String evaluateGlobalPlaceholder(@NotNull String key, @Nullable Player player) {
        try {
            if (player != null) {
                var playerValue = this.globalPlaceholderRegistry.getPlayer(key, player);
                if (playerValue.isPresent()) {
                    return playerValue.get();
                }
            }
            var value = this.globalPlaceholderRegistry.get(key);
            return value.orElseGet(() -> "%" + key + "%");
        } catch (Exception e) {
            Logger.warn("Error resolving global placeholder '%key%': %error%",
                    Placeholder.of("key", key, "error", e.getMessage()));
            return String.format(ERROR_FORMAT, "%" + key + "%");
        }
    }
}
