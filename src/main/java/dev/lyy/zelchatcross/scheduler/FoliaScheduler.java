package dev.lyy.zelchatcross.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Platform scheduler implementation for Folia regionized servers.
 */
public final class FoliaScheduler implements PlatformScheduler {

    private final Plugin plugin;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
    }

    @Override
    public TaskHandle runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit) {
        ScheduledTask task = Bukkit.getAsyncScheduler().runDelayed(plugin, t -> runnable.run(), delay, unit);
        return task::cancel;
    }

    @Override
    public TaskHandle runAsyncRepeating(Runnable runnable, long initialDelay, long interval, TimeUnit unit) {
        ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> runnable.run(), initialDelay, interval, unit);
        return task::cancel;
    }

    @Override
    public void runGlobal(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
    }

    @Override
    public void runEntity(Entity entity, Runnable runnable) {
        if (entity != null && entity.isValid()) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
        }
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }
}
