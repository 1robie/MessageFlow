package fr.robie.messageflow.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Folia's schedulers. Only loaded on Folia, so its API is never touched on other servers.
 */
final class FoliaTasks {

    /**
     * {@code Bukkit.isGlobalTickThread()}, or null on Folia builds older than it (1.20.6): calling it directly
     * would throw {@code NoSuchMethodError} there.
     */
    private static final @Nullable MethodHandle IS_GLOBAL_TICK_THREAD = findIsGlobalTickThread();

    private FoliaTasks() {
    }

    private static @Nullable MethodHandle findIsGlobalTickThread() {
        try {
            return MethodHandles.publicLookup().findStatic(Bukkit.class, "isGlobalTickThread", MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    static boolean isOwner(@NotNull Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    /**
     * Checks if the current thread is the global region's. Where Folia can't tell, says no: the task is then
     * scheduled on the global region, which is always safe, one tick later.
     */
    static boolean isGlobalThread() {
        if (IS_GLOBAL_TICK_THREAD == null) {
            return false;
        }
        try {
            return (boolean) IS_GLOBAL_TICK_THREAD.invokeExact();
        } catch (Throwable throwable) {
            return false;
        }
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
