package dev.lyy.zelchatcross.scheduler;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Universal platform scheduler interface supporting both Folia regionized threading and Paper/Bukkit.
 */
public interface PlatformScheduler {

    /**
     * Creates the appropriate platform scheduler based on the runtime environment.
     *
     * @param plugin the plugin instance
     * @return the platform scheduler
     */
    static PlatformScheduler create(Plugin plugin) {
        if (isFolia()) {
            return new FoliaScheduler(plugin);
        } else {
            return new PaperScheduler(plugin);
        }
    }

    /**
     * Checks if the server is running on Folia.
     *
     * @return true if running Folia
     */
    static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Executes a task asynchronously.
     *
     * @param runnable the task to run
     */
    void runAsync(Runnable runnable);

    /**
     * Schedules a task to run asynchronously after a delay.
     *
     * @param runnable the task to run
     * @param delay    the delay
     * @param unit     the time unit
     * @return the task handle
     */
    TaskHandle runAsyncDelayed(Runnable runnable, long delay, TimeUnit unit);

    /**
     * Schedules a task to run asynchronously at fixed intervals.
     *
     * @param runnable the task to run
     * @param initialDelay initial delay
     * @param interval     interval between runs
     * @param unit         the time unit
     * @return the task handle
     */
    TaskHandle runAsyncRepeating(Runnable runnable, long initialDelay, long interval, TimeUnit unit);

    /**
     * Runs a task on the global region / main server thread.
     *
     * @param runnable the task to run
     */
    void runGlobal(Runnable runnable);

    /**
     * Runs a task on the region / thread owning the specified entity.
     *
     * @param entity   the entity
     * @param runnable the task to run
     */
    void runEntity(Entity entity, Runnable runnable);

    /**
     * Cancels all scheduled tasks for this plugin.
     */
    void cancelAllTasks();
}
