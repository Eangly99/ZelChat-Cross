package dev.lyy.zelchatcross.redis.payload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payload for network-wide player presence synchronization.
 */
public final class PresencePayload extends RedisPayload {

    public enum Type {
        PLAYER_JOIN,
        PLAYER_QUIT,
        SERVER_HEARTBEAT,
        SERVER_STOP,
        SYNC_REQUEST
    }

    private final Type type;
    private final String serverDisplayName;
    private final UUID singlePlayerUuid;
    private final String singlePlayerName;
    private final Map<String, String> onlinePlayers; // Map of String UUID -> PlayerName for robust JSON serialization

    public PresencePayload(String originServerId,
                           String serverDisplayName,
                           Type type,
                           UUID singlePlayerUuid,
                           String singlePlayerName,
                           Map<String, String> onlinePlayers) {
        super(originServerId);
        this.serverDisplayName = serverDisplayName;
        this.type = type;
        this.singlePlayerUuid = singlePlayerUuid;
        this.singlePlayerName = singlePlayerName;
        this.onlinePlayers = onlinePlayers != null ? new HashMap<>(onlinePlayers) : new HashMap<>();
    }

    public Type getType() {
        return type;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public UUID getSinglePlayerUuid() {
        return singlePlayerUuid;
    }

    public String getSinglePlayerName() {
        return singlePlayerName;
    }

    public Map<String, String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public static PresencePayload fromJson(String json) {
        return GSON.fromJson(json, PresencePayload.class);
    }
}
