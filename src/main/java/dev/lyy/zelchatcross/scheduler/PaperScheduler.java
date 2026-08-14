package dev.lyy.zelchatcross.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * Platform scheduler implementation for Paper and Spigot servers.
 */
public final class PaperScheduler implements PlatformScheduler {

    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public TaskHandle runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        long ticks = Math.max(1L, unit.toMillis(delay) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticks);
        return task::cancel;
    }

    @Override
    public TaskHandle runAsyncRepeating(Runnable runnable, long initialDelay, long interval, TimeUnit unit) {
        long initialTicks = Math.max(0L, unit.toMillis(initialDelay) / 50L);
        long intervalTicks = Math.max(1L, unit.toMillis(interval) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialTicks, intervalTicks);
        return task::cancel;
    }

    @Override
    public void runGlobal(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    @Override
    public void runEntity(Entity entity, Runnable runnable) {
        runGlobal(runnable);
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
