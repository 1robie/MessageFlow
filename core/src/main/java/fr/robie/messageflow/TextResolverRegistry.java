package fr.robie.messageflow;

import com.google.common.base.Preconditions;
import fr.robie.messageflow.api.ITextResolverRegistry;
import fr.robie.messageflow.api.TextResolver;
import fr.robie.messageflow.configuration.ConfigurationManager;
import fr.robie.messageflow.formatter.FunctionalPlaceholderResolver;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.hooks.placeholderapi.PlaceholderAPIResolver;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;

public class TextResolverRegistry implements ITextResolverRegistry {
    private final CopyOnWriteArrayList<TextResolver> resolvers = new CopyOnWriteArrayList<>();

    @Override
    public void initialize() {
        this.register(new FunctionalPlaceholderResolver());
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.register(new PlaceholderAPIResolver());
            if (ConfigurationManager.Setting.HOOK_PLACEHOLDER_API_LOG_ON_LOAD.getValue()) {
                Logger.info(ConfigurationManager.Setting.HOOK_LOAD_MESSAGE.<String>getValue(), Placeholder.of("hook", "PlaceholderAPI"));
            }
        }
    }

    @Override
    public void register(@NotNull TextResolver resolver) {
        Preconditions.checkNotNull(resolver, "resolver cannot be null");
        this.resolvers.addIfAbsent(resolver);
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
    public boolean canResolve(@NotNull String text, @Nullable Player player, @NotNull Placeholder placeholders) {
        for (TextResolver resolver : this.resolvers) {
            if (resolver.canResolve(text, player, placeholders)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPlayerDependent(@NotNull String text, @NotNull Placeholder placeholders) {
        for (TextResolver resolver : this.resolvers) {
            if (resolver.isPlayerDependent(text, placeholders)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasResolvers() {
        return !this.resolvers.isEmpty();
    }
}
