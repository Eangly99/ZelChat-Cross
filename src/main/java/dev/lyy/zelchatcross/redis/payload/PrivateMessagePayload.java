package dev.lyy.zelchatcross.redis.payload;

import java.util.UUID;

/**
 * Payload for cross-server 1-to-1 private messages (/msg, /tell, /r).
 */
public final class PrivateMessagePayload extends RedisPayload {

    private final UUID senderUuid;
    private final String senderName;
    private final String senderServerDisplayName;
    private final UUID targetUuid;
    private final String targetName;
    private final String message;

    public PrivateMessagePayload(String originServerId,
                                 UUID senderUuid,
                                 String senderName,
                                 String senderServerDisplayName,
                                 UUID targetUuid,
                                 String targetName,
                                 String message) {
        super(originServerId);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.senderServerDisplayName = senderServerDisplayName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.message = message;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderServerDisplayName() {
        return senderServerDisplayName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getMessage() {
        return message;
    }

    public static PrivateMessagePayload fromJson(String json) {
        return GSON.fromJson(json, PrivateMessagePayload.class);
    }
}
