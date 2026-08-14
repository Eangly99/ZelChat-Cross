package dev.lyy.zelchatcross.redis.payload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

/**
 * Base abstract class for all Redis Pub/Sub messages.
 * Includes custom UUID TypeAdapter and complex map key support for cross-server robustness.
 */
public abstract class RedisPayload {

    public static final Gson GSON = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(UUID.class, new TypeAdapter<UUID>() {
                @Override
                public void write(JsonWriter out, UUID value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.toString());
                    }
                }

                @Override
                public UUID read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    return UUID.fromString(in.nextString());
                }
            })
            .create();

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
