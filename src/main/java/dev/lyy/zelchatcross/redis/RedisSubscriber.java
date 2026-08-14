package dev.lyy.zelchatcross.redis;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

/**
 * Resilient background daemon subscriber for Redis Pub/Sub channels.
 */
public final class RedisSubscriber {

    private final ZelChatCross plugin;
    private final RedisManager redisManager;
    private Thread subscriberThread;
    private JedisPubSub pubSub;
    private volatile boolean running = false;

    public RedisSubscriber(ZelChatCross plugin, RedisManager redisManager) {
        this.plugin = plugin;
        this.redisManager = redisManager;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        this.running = true;

        this.pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (!running) return;
                try {
                    handleIncomingMessage(channel, message);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[Redis] Error processing incoming payload on "
                            + channel + ": " + e.getMessage(), e);
                }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                plugin.getLogger().info("[Redis] Subscribed to channel: " + channel);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                plugin.getLogger().info("[Redis] Unsubscribed from channel: " + channel);
            }
        };

        this.subscriberThread = new Thread(() -> {
            int backoffSeconds = 1;
            while (running) {
                try {
                    String[] channels = redisManager.getChannels().allChannels();
                    plugin.getLogger().info("[Redis] Starting Pub/Sub subscription loop for channels: " + String.join(", ", channels));
                    try (Jedis jedis = redisManager.getResource()) {
                        backoffSeconds = 1; // Reset backoff on successful connection
                        jedis.subscribe(pubSub, channels);
                    }
                } catch (Exception e) {
                    if (running) {
                        plugin.getLogger().warning("[Redis] Pub/Sub connection lost (" + e.getMessage()
                                + "). Reconnecting in " + backoffSeconds + "s...");
                        try {
                            Thread.sleep(backoffSeconds * 1000L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        backoffSeconds = Math.min(10, backoffSeconds * 2);
                    }
                }
            }
        }, "ZelCross-RedisSubscriber");
        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
    }

    public synchronized void stop() {
        this.running = false;
        if (pubSub != null && pubSub.isSubscribed()) {
            try {
                pubSub.unsubscribe();
            } catch (Exception ignored) {
            }
        }
        if (subscriberThread != null && subscriberThread.isAlive()) {
            subscriberThread.interrupt();
        }
    }

    private void handleIncomingMessage(String channel, String message) {
        RedisChannel rc = redisManager.getChannels();
        String currentServerId = plugin.getConfigManager().getServerId();

        if (channel.equals(rc.chat())) {
            ChatMessagePayload payload = ChatMessagePayload.fromJson(message);
            if (payload == null) return;

            if (currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("[Debug] Dropped incoming chat from " + payload.getOriginServerId()
                            + " (matches local server-id '" + currentServerId + "')");
                }
                return; // Local echo prevention
            }

            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[Debug] Received chat payload from origin: "
                        + payload.getOriginServerId() + " for player " + payload.getSenderName());
            }
            plugin.getChatManager().handleIncomingChat(payload);
        } else if (channel.equals(rc.privateMessage())) {
            PrivateMessagePayload payload = PrivateMessagePayload.fromJson(message);
            if (payload == null || currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                return; // Local echo prevention
            }
            plugin.getPrivateMessageManager().handleIncomingPrivateMessage(payload);
        } else if (channel.equals(rc.presence())) {
            PresencePayload payload = PresencePayload.fromJson(message);
            if (payload == null || currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                return; // Local echo prevention
            }
            plugin.getPresenceManager().handleIncomingPresence(payload);
        } else if (channel.equals(rc.moderation())) {
            ModerationPayload payload = ModerationPayload.fromJson(message);
            if (payload == null || currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                return; // Local echo prevention
            }
            plugin.getModerationManager().handleIncomingModeration(payload);
        } else if (channel.equals(rc.showcase())) {
            ShowcaseSnapshotPayload payload = ShowcaseSnapshotPayload.fromJson(message);
            if (payload == null || currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                return; // Local echo prevention
            }
            plugin.getShowcaseManager().handleIncomingSnapshot(payload);
        } else if (channel.equals(rc.staffChat())) {
            StaffChatPayload payload = StaffChatPayload.fromJson(message);
            if (payload == null || currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                return; // Local echo prevention
            }
            plugin.getChatManager().handleIncomingStaffChat(payload);
        } else if (channel.equals(rc.spy())) {
            SocialSpyPayload payload = SocialSpyPayload.fromJson(message);
            if (payload == null || currentServerId.equalsIgnoreCase(payload.getOriginServerId())) {
                return; // Local echo prevention
            }
            plugin.getPrivateMessageManager().handleIncomingSpyMessage(payload);
        }
    }
}
