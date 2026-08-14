package dev.lyy.zelchatcross.redis.payload;

import java.util.UUID;

/**
 * Payload for network-wide staff chat messages.
 */
public final class StaffChatPayload extends RedisPayload {

    private final UUID senderUuid;
    private final String senderName;
    private final String originServerDisplayName;
    private final String message;

    public StaffChatPayload(String originServerId,
                            UUID senderUuid,
                            String senderName,
                            String originServerDisplayName,
                            String message) {
        super(originServerId);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.originServerDisplayName = originServerDisplayName;
        this.message = message;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getOriginServerDisplayName() {
        return originServerDisplayName;
    }

    public String getMessage() {
        return message;
    }

    public static StaffChatPayload fromJson(String json) {
        return GSON.fromJson(json, StaffChatPayload.class);
    }
}
