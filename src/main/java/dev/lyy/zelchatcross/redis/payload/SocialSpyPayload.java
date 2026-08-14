package dev.lyy.zelchatcross.redis.payload;

import java.util.UUID;

/**
 * Payload for network-wide SocialSpy private message snooping for authorized staff.
 */
public final class SocialSpyPayload extends RedisPayload {

    private final UUID senderUuid;
    private final String senderName;
    private final String senderServerDisplayName;
    private final UUID targetUuid;
    private final String targetName;
    private final String targetServerDisplayName;
    private final String message;

    public SocialSpyPayload(String originServerId,
                            UUID senderUuid,
                            String senderName,
                            String senderServerDisplayName,
                            UUID targetUuid,
                            String targetName,
                            String targetServerDisplayName,
                            String message) {
        super(originServerId);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.senderServerDisplayName = senderServerDisplayName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetServerDisplayName = targetServerDisplayName;
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

    public String getTargetServerDisplayName() {
        return targetServerDisplayName;
    }

    public String getMessage() {
        return message;
    }

    public static SocialSpyPayload fromJson(String json) {
        return GSON.fromJson(json, SocialSpyPayload.class);
    }
}
