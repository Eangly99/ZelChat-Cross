package dev.lyy.zelchatcross.redis;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.*;
import redis.clients.jedis.Jedis;

import java.util.logging.Level;

/**
 * Handles asynchronous message publishing to Redis Pub/Sub channels.
 */
public final class RedisPublisher {

    private final ZelChatCross plugin;
    private final RedisManager redisManager;

    public RedisPublisher(ZelChatCross plugin, RedisManager redisManager) {
        this.plugin = plugin;
        this.redisManager = redisManager;
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
        plugin.getScheduler().runAsync(() -> {
            try {
                if (!redisManager.isConnected()) {
                    return;
                }
                try (Jedis jedis = redisManager.getResource()) {
                    jedis.publish(channel, message);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Redis] Failed to publish message to channel "
                        + channel + ": " + e.getMessage());
            }
        });
    }
}
