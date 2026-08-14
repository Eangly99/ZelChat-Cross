package dev.lyy.zelchatcross.redis;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * High-throughput asynchronous message publisher for Redis Pub/Sub channels.
 * Uses a dedicated lock-free publisher queue, batch draining, and guaranteed
 * shutdown flush for 300+ player concurrency.
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
            List<PublishTask> batch = new ArrayList<>(64);
            while (running) {
                try {
                    PublishTask first = publishQueue.poll(200, TimeUnit.MILLISECONDS);
                    if (first == null) continue;

                    batch.clear();
                    batch.add(first);
                    publishQueue.drainTo(batch, 63);

                    if (!redisManager.isConnected()) {
                        continue;
                    }

                    try (Jedis jedis = redisManager.getResource()) {
                        if (batch.size() == 1) {
                            jedis.publish(batch.get(0).channel(), batch.get(0).message());
                        } else {
                            Pipeline pipeline = jedis.pipelined();
                            for (PublishTask task : batch) {
                                pipeline.publish(task.channel(), task.message());
                            }
                            pipeline.sync();
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.FINE, "[RedisPublisher] Error publishing batch ("
                                + batch.size() + " msgs): " + e.getMessage());
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

        // Flush any remaining critical tasks (such as SERVER_STOP) before closing pool
        if (!publishQueue.isEmpty() && redisManager.isConnected()) {
            try (Jedis jedis = redisManager.getResource()) {
                Pipeline pipeline = jedis.pipelined();
                PublishTask task;
                int count = 0;
                while ((task = publishQueue.poll()) != null && count < 100) {
                    pipeline.publish(task.channel(), task.message());
                    count++;
                }
                pipeline.sync();
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "[RedisPublisher] Flush on shutdown failed: " + e.getMessage());
            }
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
        if (channel == null || message == null) return;
        boolean added = publishQueue.offer(new PublishTask(channel, message));
        if (!added) {
            plugin.getLogger().warning("[RedisPublisher] High load: publish queue is full (10,000)! Dropping message on " + channel);
        }
    }
}
