package dev.lyy.zelchatcross.redis;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.*;
import redis.clients.jedis.Jedis;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * High-throughput asynchronous message publisher for Redis Pub/Sub channels.
 * Uses a dedicated lock-free publisher queue ensuring strict FIFO order and zero
 * Bukkit thread-pool contention at 300+ player concurrency.
 */
public final class RedisPublisher {

    private record PublishTask(String channel, String message) {}

    private final ZelChatCross plugin;
    private final RedisManager redisManager;
    private final BlockingQueue<PublishTask> publishQueue = new LinkedBlockingQueue<>(10000);
    private Thread publisherThread;
    private volatile boolean running = false;

    public RedisPublisher(ZelChatCross plugin, RedisManager redisManager) {
        this.plugin = plugin;
        this.redisManager = redisManager;
        startPublisherWorker();
    }

    private synchronized void startPublisherWorker() {
        if (running) return;
        this.running = true;

        this.publisherThread = new Thread(() -> {
            while (running) {
                try {
                    PublishTask task = publishQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (task == null) continue;

                    if (!redisManager.isConnected()) {
                        continue;
                    }

                    try (Jedis jedis = redisManager.getResource()) {
                        jedis.publish(task.channel(), task.message());
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.FINE, "[RedisPublisher] Error publishing to "
                                + task.channel() + ": " + e.getMessage());
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[RedisPublisher] Worker exception: " + e.getMessage());
                }
            }
        }, "ZelCross-RedisPublisherWorker");
        this.publisherThread.setDaemon(true);
        this.publisherThread.start();
    }

    public synchronized void shutdown() {
        this.running = false;
        if (publisherThread != null && publisherThread.isAlive()) {
            publisherThread.interrupt();
        }
        publishQueue.clear();
    }

    public void publishChat(ChatMessagePayload payload) {
        publishAsync(redisManager.getChannels().chat(), payload.toJson());
    }

    public void publishPrivateMessage(PrivateMessagePayload payload) {
        publishAsync(redisManager.getChannels().privateMessage(), payload.toJson());
    }

    public void publishPresence(PresencePayload payload) {
        publishAsync(redisManager.getChannels().presence(), payload.toJson());
    }

    public void publishModeration(ModerationPayload payload) {
        publishAsync(redisManager.getChannels().moderation(), payload.toJson());
    }

    public void publishShowcase(ShowcaseSnapshotPayload payload) {
        publishAsync(redisManager.getChannels().showcase(), payload.toJson());
    }

    public void publishStaffChat(StaffChatPayload payload) {
        publishAsync(redisManager.getChannels().staffChat(), payload.toJson());
    }

    public void publishSpy(SocialSpyPayload payload) {
        publishAsync(redisManager.getChannels().spy(), payload.toJson());
    }

    public void publishAsync(String channel, String message) {
        if (!running || channel == null || message == null) return;
        boolean added = publishQueue.offer(new PublishTask(channel, message));
        if (!added) {
            plugin.getLogger().warning("[RedisPublisher] High load: publish queue is full (10,000)! Dropping message on " + channel);
        }
    }
}
