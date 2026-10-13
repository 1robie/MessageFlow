package fr.robie.messageflow.util;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Runs work on the right thread, on Paper, Spigot and Folia alike: the main thread, or on Folia the thread of the
 * region that owns an entity (the global region for anything else, such as the console).
 */
public final class Tasks {

    private Tasks() {
    }

    /**
     * Checks if the current thread may change an entity (its region thread on Folia, the main thread otherwise).
     *
     * @param entity the entity
     * @return true when the entity can be used now
     */
    public static boolean isOwner(@NotNull Entity entity) {
        return PlatformType.isFolia() ? FoliaTasks.isOwner(entity) : Bukkit.isPrimaryThread();
    }

    /**
     * Runs a task for a target now when the current thread owns it, otherwise as soon as possible on its thread.
     *
     * @param plugin the plugin owning the task
     * @param target an entity (run on its thread) or anything else (run on the main / global thread)
     * @param task   the task
     */
    public static void run(@NotNull Plugin plugin, @NotNull Object target, @NotNull Runnable task) {
        if (target instanceof Entity entity ? isOwner(entity) : isGlobalThread()) {
            task.run();
        } else if (PlatformType.isFolia()) {
            FoliaTasks.run(plugin, target, task);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task for a target after a delay, on its thread.
     *
     * @param plugin the plugin owning the task
     * @param target an entity (run on its thread) or anything else (run on the main / global thread)
     * @param ticks  the delay in ticks
     * @param task   the task
     */
    public static void runLater(@NotNull Plugin plugin, @NotNull Object target, long ticks, @NotNull Runnable task) {
        if (PlatformType.isFolia()) {
            FoliaTasks.runLater(plugin, target, ticks, task);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
        }
    }

    /**
     * Runs a task on the main thread (the global region on Folia) after a delay.
     *
     * @param plugin the plugin owning the task
     * @param ticks  the delay in ticks
     * @param task   the task
     */
    public static void runGlobalLater(@NotNull Plugin plugin, long ticks, @NotNull Runnable task) {
        if (PlatformType.isFolia()) {
            FoliaTasks.runGlobalLater(plugin, ticks, task);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
        }
    }

    /**
     * Runs a task off the main thread.
     *
     * @param plugin the plugin owning the task
     * @param task   the task
     * @return completes when the task is done, exceptionally if it throws
     */
    public static @NotNull CompletableFuture<Void> runAsync(@NotNull Plugin plugin, @NotNull Runnable task) {
        Preconditions.checkNotNull(task, "Task cannot be null");
        CompletableFuture<Void> future = new CompletableFuture<>();
        Runnable completing = () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        };
        if (PlatformType.isFolia()) {
            FoliaTasks.runAsync(plugin, completing);
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, completing);
        }
        return future;
    }

    private static boolean isGlobalThread() {
        return PlatformType.isFolia() ? FoliaTasks.isGlobalThread() : Bukkit.isPrimaryThread();
    }
}
