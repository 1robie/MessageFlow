package fr.robie.messageflow.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ITextResolverRegistry {
    void initialize();

    void register(@NotNull TextResolver resolver);

    @NotNull String resolve(@NotNull String text, @Nullable Player player, Object... args);

    boolean hasResolvers();
}
