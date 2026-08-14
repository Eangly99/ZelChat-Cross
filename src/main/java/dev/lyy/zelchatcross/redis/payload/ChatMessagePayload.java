package dev.lyy.zelchatcross.redis.payload;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Payload for cross-server chat messages.
 */
public final class ChatMessagePayload extends RedisPayload {

    private final UUID senderUuid;
    private final String senderName;
    private final String originServerDisplayName;
    private final String channelType; // EVERYONE, STAFF, CUSTOM
    private final String channelName;
    private final String rawMessage;
    private final String miniMessageContent;
    private final List<String> mentions;

    public ChatMessagePayload(String originServerId,
                              UUID senderUuid,
                              String senderName,
                              String originServerDisplayName,
                              String channelType,
                              String channelName,
                              String rawMessage,
                              String miniMessageContent,
                              Collection<String> mentions) {
        super(originServerId);
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.originServerDisplayName = originServerDisplayName;
        this.channelType = channelType;
        this.channelName = channelName;
        this.rawMessage = rawMessage;
        this.miniMessageContent = miniMessageContent;
        this.mentions = mentions != null ? List.copyOf(mentions) : List.of();
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

    public String getChannelType() {
        return channelType;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getMiniMessageContent() {
        return miniMessageContent;
    }

    public List<String> getMentions() {
        return mentions;
    }

    public static ChatMessagePayload fromJson(String json) {
        return GSON.fromJson(json, ChatMessagePayload.class);
    }
}
