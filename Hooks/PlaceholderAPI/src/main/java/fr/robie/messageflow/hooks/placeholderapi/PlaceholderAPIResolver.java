package fr.robie.messageflow.hooks.placeholderapi;

import fr.robie.messageflow.api.TextResolver;
import fr.robie.messageflow.formatter.Placeholder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIResolver implements TextResolver {


    @Override
    public boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        return text.contains("%");
    }

    @Override
    public @NotNull String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
