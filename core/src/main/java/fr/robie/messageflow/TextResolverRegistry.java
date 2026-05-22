package fr.robie.messageflow;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.ITextResolverRegistry;
import fr.robie.messageflow.api.TextResolver;
import fr.robie.messageflow.formatter.FunctionalPlaceholderResolver;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.hooks.placeholderapi.PlaceholderAPIResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class TextResolverRegistry implements ITextResolverRegistry {
    private final Set<TextResolver> resolvers = new HashSet<>();

    @Override
    public void initialize() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.register(new PlaceholderAPIResolver());
        }
        this.register(new FunctionalPlaceholderResolver());
    }

    @Override
    public void register(@NotNull TextResolver resolver) {
        Preconditions.checkNotNull(resolver, "resolver cannot be null");
        this.resolvers.add(resolver);
    }

    @Override
    public @NotNull String resolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        for (TextResolver resolver : this.resolvers) {
            if (resolver.canResolve(text, player, placeholders)) {
                text = resolver.resolve(text, player, placeholders);
            }
        }
        return text;
    }

    @Override
    public boolean hasResolvers() {
        return !this.resolvers.isEmpty();
    }
}
