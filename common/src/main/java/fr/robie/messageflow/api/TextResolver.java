package fr.robie.messageflow.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TextResolver {

    boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Object... args);

    @NotNull
    String resolve(@NotNull String text, @Nullable Player player, @NotNull Object... args);

}
