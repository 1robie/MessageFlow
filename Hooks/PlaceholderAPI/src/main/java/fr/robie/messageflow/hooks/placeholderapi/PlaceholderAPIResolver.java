package fr.robie.messageflow.hooks.placeholderapi;

import fr.robie.messageflow.api.TextResolver;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.util.PlaceholderScanner;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIResolver implements TextResolver {

    /**
     * True when the text has a {@code %identifier_params%} token whose expansion is registered, so plain
     * percent signs ({@code 50% off}) don't send each message through PlaceholderAPI for every player.
     */
    @Override
    public boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        return PlaceholderScanner.anyKey(text, PlaceholderAPIResolver::isExpansion);
    }

    /**
     * Every PlaceholderAPI expansion may depend on the player: per player whenever one applies.
     */
    @Override
    public boolean isPlayerDependent(@NotNull String text, @NotNull Placeholder placeholders) {
        return this.canResolve(text, null, placeholders);
    }

    @Override
    public @NotNull String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    private static boolean isExpansion(@NotNull String key) {
        int separator = key.indexOf('_');
        String identifier = separator < 0 ? key : key.substring(0, separator);
        return !identifier.isEmpty() && identifier.indexOf(' ') < 0 && PlaceholderAPI.isRegistered(identifier);
    }
}
