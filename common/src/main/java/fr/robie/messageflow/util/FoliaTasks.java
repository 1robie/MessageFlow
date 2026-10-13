package fr.robie.messageflow.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Folia's schedulers. Only loaded on Folia, so its API is never touched on other servers.
 */
final class FoliaTasks {

    private FoliaTasks() {
    }

    static boolean isOwner(@NotNull Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    static boolean isGlobalThread() {
        return Bukkit.isGlobalTickThread();
    }

    static void run(@NotNull Plugin plugin, @NotNull Object target, @NotNull Runnable task) {
        if (target instanceof Entity entity) {
            // Not run when the entity is removed first (a player who left): there is nothing to update then.
            entity.getScheduler().run(plugin, scheduled -> task.run(), null);
        } else {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduled -> task.run());
        }
    }

    static void runLater(@NotNull Plugin plugin, @NotNull Object target, long ticks, @NotNull Runnable task) {
        long delay = Math.max(1L, ticks);
        if (target instanceof Entity entity) {
            entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, delay);
        } else {
            runGlobalLater(plugin, delay, task);
        }
    }

    static void runGlobalLater(@NotNull Plugin plugin, long ticks, @NotNull Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduled -> task.run(), Math.max(1L, ticks));
    }

    static void runAsync(@NotNull Plugin plugin, @NotNull Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduled -> task.run());
    }
}
