package dev.lyy.zelchatcross.redis.payload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.UUID;

/**
 * Base abstract class for all Redis Pub/Sub messages.
 */
public abstract class RedisPayload {

    public static final Gson GSON = new GsonBuilder().create();

    private final String messageId;
    private final String originServerId;
    private final long timestamp;

    public RedisPayload(String originServerId) {
        this.messageId = UUID.randomUUID().toString();
        this.originServerId = originServerId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessageId() {
        return messageId;
    }

    public String getOriginServerId() {
        return originServerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}
