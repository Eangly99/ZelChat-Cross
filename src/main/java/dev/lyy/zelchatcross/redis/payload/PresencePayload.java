package dev.lyy.zelchatcross.redis.payload;

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
        SERVER_STOP
    }

    private final Type type;
    private final String serverDisplayName;
    private final UUID singlePlayerUuid;
    private final String singlePlayerName;
    private final Map<UUID, String> onlinePlayers; // Used for HEARTBEAT snapshot

    public PresencePayload(String originServerId,
                           String serverDisplayName,
                           Type type,
                           UUID singlePlayerUuid,
                           String singlePlayerName,
                           Map<UUID, String> onlinePlayers) {
        super(originServerId);
        this.serverDisplayName = serverDisplayName;
        this.type = type;
        this.singlePlayerUuid = singlePlayerUuid;
        this.singlePlayerName = singlePlayerName;
        this.onlinePlayers = onlinePlayers != null ? Map.copyOf(onlinePlayers) : Map.of();
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

    public Map<UUID, String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public static PresencePayload fromJson(String json) {
        return GSON.fromJson(json, PresencePayload.class);
    }
}
