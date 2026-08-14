package dev.lyy.zelchatcross.redis;

/**
 * Defines the Redis Pub/Sub channels used across the network.
 */
public final class RedisChannel {

    private final String prefix;

    public RedisChannel(String prefix) {
        this.prefix = prefix == null || prefix.isEmpty() ? "zelcross" : prefix;
    }

    public String chat() {
        return prefix + ":chat";
    }

    public String privateMessage() {
        return prefix + ":pm";
    }

    public String presence() {
        return prefix + ":presence";
    }

    public String moderation() {
        return prefix + ":moderation";
    }

    public String showcase() {
        return prefix + ":showcase";
    }

    public String staffChat() {
        return prefix + ":staffchat";
    }

    public String spy() {
        return prefix + ":spy";
    }

    public String[] allChannels() {
        return new String[]{
                chat(),
                privateMessage(),
                presence(),
                moderation(),
                showcase(),
                staffChat(),
                spy()
        };
    }
}
