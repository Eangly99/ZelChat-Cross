package dev.lyy.zelchatcross.presence;

import java.util.UUID;

/**
 * Represents a player currently online anywhere on the network.
 */
public final class NetworkPlayer {

    private final UUID uniqueId;
    private final String username;
    private final String serverId;
    private final String serverDisplayName;
    private volatile long lastSeen;

    public NetworkPlayer(UUID uniqueId, String username, String serverId, String serverDisplayName) {
        this.uniqueId = uniqueId;
        this.username = username;
        this.serverId = serverId;
        this.serverDisplayName = serverDisplayName;
        this.lastSeen = System.currentTimeMillis();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getUsername() {
        return username;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerDisplayName() {
        return serverDisplayName;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
}
