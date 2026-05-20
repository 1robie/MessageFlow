package fr.robie.messageflow.api;

import fr.robie.messageflow.formatter.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TextResolver {

    boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders);

    @NotNull
    String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders);

}
