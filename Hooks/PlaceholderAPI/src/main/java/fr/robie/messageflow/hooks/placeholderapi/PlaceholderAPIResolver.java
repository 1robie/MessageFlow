package fr.robie.messageflow.hooks.placeholderapi;

import fr.robie.messageflow.api.TextResolver;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class PlaceholderAPIResolver implements TextResolver {


    @Override
    public boolean canResolve(@NotNull String text, @Nullable Player player, @NonNull @NotNull Object... args) {
        return text.contains("%");
    }

    @Override
    public @NotNull String resolve(@NotNull String text, @Nullable Player player, @NonNull @NotNull Object... args) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
