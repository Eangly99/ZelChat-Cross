package dev.lyy.zelchatcross.scheduler;

/**
 * Handle to a scheduled task that can be cancelled.
 */
@FunctionalInterface
public interface TaskHandle {
    /**
     * Cancels the scheduled task.
     */
    void cancel();
}
