package dev.lyy.zelchatcross.presence;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.PresencePayload;
import dev.lyy.zelchatcross.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages distributed player presence caching across all servers.
 */
public final class NetworkPresenceManager {

    private final ZelChatCross plugin;
    private final Map<UUID, NetworkPlayer> playersByUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> uuidByName = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> playersByServer = new ConcurrentHashMap<>();
    private final Map<String, Long> serverHeartbeats = new ConcurrentHashMap<>();
    private final Map<String, String> serverDisplayNames = new ConcurrentHashMap<>();

    private TaskHandle heartbeatTask;
    private TaskHandle cleanupTask;

    public NetworkPresenceManager(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int interval = plugin.getConfigManager().getPresenceHeartbeatInterval();

        // Register initial local players
        for (Player p : Bukkit.getOnlinePlayers()) {
            registerLocalPlayer(p);
        }

        // Send initial heartbeat and start recurring task
        sendHeartbeat();
        this.heartbeatTask = plugin.getScheduler().runAsyncRepeating(
                this::sendHeartbeat,
                interval,
                interval,
                TimeUnit.SECONDS
        );

        // Periodic cleanup task for timed out servers
        this.cleanupTask = plugin.getScheduler().runAsyncRepeating(
                this::cleanupDeadServers,
                interval * 2L,
                interval * 2L,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        // Broadcast server stop
        String serverId = plugin.getConfigManager().getServerId();
        PresencePayload stopPayload = new PresencePayload(
                serverId,
                plugin.getConfigManager().getServerDisplayName(),
                PresencePayload.Type.SERVER_STOP,
                null,
                null,
                null
        );
        plugin.getRedisManager().getPublisher().publishPresence(stopPayload);

        playersByUuid.clear();
        uuidByName.clear();
        playersByServer.clear();
        serverHeartbeats.clear();
    }

    public void onLocalPlayerJoin(Player player) {
        registerLocalPlayer(player);

        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplay = plugin.getConfigManager().getServerDisplayName();

        PresencePayload payload = new PresencePayload(
                serverId,
                serverDisplay,
                PresencePayload.Type.PLAYER_JOIN,
                player.getUniqueId(),
                player.getName(),
                null
        );
        plugin.getRedisManager().getPublisher().publishPresence(payload);
    }

    public void onLocalPlayerQuit(Player player) {
        unregisterLocalPlayer(player.getUniqueId(), player.getName());

        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplay = plugin.getConfigManager().getServerDisplayName();

        PresencePayload payload = new PresencePayload(
                serverId,
                serverDisplay,
                PresencePayload.Type.PLAYER_QUIT,
                player.getUniqueId(),
                player.getName(),
                null
        );
        plugin.getRedisManager().getPublisher().publishPresence(payload);
    }

    private void registerLocalPlayer(Player player) {
        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplay = plugin.getConfigManager().getServerDisplayName();

        NetworkPlayer netPlayer = new NetworkPlayer(player.getUniqueId(), player.getName(), serverId, serverDisplay);
        playersByUuid.put(player.getUniqueId(), netPlayer);
        uuidByName.put(player.getName().toLowerCase(Locale.ROOT), player.getUniqueId());

        playersByServer.computeIfAbsent(serverId, k -> ConcurrentHashMap.newKeySet()).add(player.getUniqueId());
        serverDisplayNames.put(serverId, serverDisplay);
        serverHeartbeats.put(serverId, System.currentTimeMillis());
    }

    private void unregisterLocalPlayer(UUID uuid, String name) {
        String serverId = plugin.getConfigManager().getServerId();
        playersByUuid.remove(uuid);
        if (name != null) {
            uuidByName.remove(name.toLowerCase(Locale.ROOT));
        }
        Set<UUID> serverSet = playersByServer.get(serverId);
        if (serverSet != null) {
            serverSet.remove(uuid);
        }
    }

    private void sendHeartbeat() {
        try {
            String serverId = plugin.getConfigManager().getServerId();
            String serverDisplay = plugin.getConfigManager().getServerDisplayName();

            Map<UUID, String> localPlayers = new HashMap<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                localPlayers.put(p.getUniqueId(), p.getName());
            }

            PresencePayload payload = new PresencePayload(
                    serverId,
                    serverDisplay,
                    PresencePayload.Type.SERVER_HEARTBEAT,
                    null,
                    null,
                    localPlayers
            );

            plugin.getRedisManager().getPublisher().publishPresence(payload);

            // Update server heartbeat TTL key in Redis for robust health check
            if (plugin.getRedisManager().isConnected()) {
                try (Jedis jedis = plugin.getRedisManager().getResource()) {
                    String key = plugin.getConfigManager().getRedisChannelPrefix() + ":server:" + serverId;
                    jedis.setex(key, plugin.getConfigManager().getPresenceServerTimeout(), String.valueOf(localPlayers.size()));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[Presence] Error sending heartbeat: " + e.getMessage());
        }
    }

    public void handleIncomingPresence(PresencePayload payload) {
        String originServer = payload.getOriginServerId();
        String serverDisplay = payload.getServerDisplayName() != null ? payload.getServerDisplayName() : originServer;
        serverDisplayNames.put(originServer, serverDisplay);
        serverHeartbeats.put(originServer, System.currentTimeMillis());

        switch (payload.getType()) {
            case PLAYER_JOIN -> {
                if (payload.getSinglePlayerUuid() != null && payload.getSinglePlayerName() != null) {
                    NetworkPlayer netPlayer = new NetworkPlayer(
                            payload.getSinglePlayerUuid(),
                            payload.getSinglePlayerName(),
                            originServer,
                            serverDisplay
                    );
                    playersByUuid.put(payload.getSinglePlayerUuid(), netPlayer);
                    uuidByName.put(payload.getSinglePlayerName().toLowerCase(Locale.ROOT), payload.getSinglePlayerUuid());
                    playersByServer.computeIfAbsent(originServer, k -> ConcurrentHashMap.newKeySet())
                            .add(payload.getSinglePlayerUuid());
                }
            }
            case PLAYER_QUIT -> {
                if (payload.getSinglePlayerUuid() != null) {
                    playersByUuid.remove(payload.getSinglePlayerUuid());
                    if (payload.getSinglePlayerName() != null) {
                        uuidByName.remove(payload.getSinglePlayerName().toLowerCase(Locale.ROOT));
                    }
                    Set<UUID> set = playersByServer.get(originServer);
                    if (set != null) {
                        set.remove(payload.getSinglePlayerUuid());
                    }
                }
            }
            case SERVER_HEARTBEAT -> {
                Map<UUID, String> remotePlayers = payload.getOnlinePlayers();
                Set<UUID> currentServerUuids = playersByServer.computeIfAbsent(originServer, k -> ConcurrentHashMap.newKeySet());

                // Remove players no longer present on that server
                Set<UUID> toRemove = new HashSet<>();
                for (UUID u : currentServerUuids) {
                    if (remotePlayers == null || !remotePlayers.containsKey(u)) {
                        toRemove.add(u);
                    }
                }
                for (UUID u : toRemove) {
                    NetworkPlayer removed = playersByUuid.remove(u);
                    if (removed != null) {
                        uuidByName.remove(removed.getUsername().toLowerCase(Locale.ROOT));
                    }
                    currentServerUuids.remove(u);
                }

                // Add or update current players
                if (remotePlayers != null) {
                    for (Map.Entry<UUID, String> entry : remotePlayers.entrySet()) {
                        UUID uuid = entry.getKey();
                        String name = entry.getValue();
                        NetworkPlayer netPlayer = new NetworkPlayer(uuid, name, originServer, serverDisplay);
                        playersByUuid.put(uuid, netPlayer);
                        uuidByName.put(name.toLowerCase(Locale.ROOT), uuid);
                        currentServerUuids.add(uuid);
                    }
                }
            }
            case SERVER_STOP -> {
                removeServer(originServer);
            }
        }
    }

    private void cleanupDeadServers() {
        long now = System.currentTimeMillis();
        long timeoutMillis = plugin.getConfigManager().getPresenceServerTimeout() * 1000L;
        String localServer = plugin.getConfigManager().getServerId();

        List<String> deadServers = new ArrayList<>();
        for (Map.Entry<String, Long> entry : serverHeartbeats.entrySet()) {
            if (entry.getKey().equals(localServer)) continue;
            if (now - entry.getValue() > timeoutMillis) {
                deadServers.add(entry.getKey());
            }
        }

        for (String deadServer : deadServers) {
            plugin.getLogger().warning("[Presence] Server '" + deadServer + "' timed out. Removing cached players.");
            removeServer(deadServer);
        }
    }

    private void removeServer(String serverId) {
        serverHeartbeats.remove(serverId);
        serverDisplayNames.remove(serverId);
        Set<UUID> players = playersByServer.remove(serverId);
        if (players != null) {
            for (UUID uuid : players) {
                NetworkPlayer removed = playersByUuid.remove(uuid);
                if (removed != null) {
                    uuidByName.remove(removed.getUsername().toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public NetworkPlayer getPlayerByName(String name) {
        if (name == null) return null;
        UUID uuid = uuidByName.get(name.toLowerCase(Locale.ROOT));
        if (uuid == null) return null;
        return playersByUuid.get(uuid);
    }

    public NetworkPlayer getPlayerByUuid(UUID uuid) {
        if (uuid == null) return null;
        return playersByUuid.get(uuid);
    }

    public Collection<NetworkPlayer> getAllOnlinePlayers() {
        return Collections.unmodifiableCollection(playersByUuid.values());
    }

    public List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>(playersByUuid.size());
        for (NetworkPlayer np : playersByUuid.values()) {
            names.add(np.getUsername());
        }
        return names;
    }

    public int getNetworkOnlineCount() {
        return playersByUuid.size();
    }

    public Map<String, Integer> getServerPlayerCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : playersByServer.entrySet()) {
            map.put(entry.getKey(), entry.getValue().size());
        }
        return map;
    }

    public String getServerDisplayName(String serverId) {
        return serverDisplayNames.getOrDefault(serverId, serverId);
    }
}
