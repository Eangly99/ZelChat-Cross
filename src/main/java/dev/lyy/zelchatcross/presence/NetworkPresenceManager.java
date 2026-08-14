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
 * Features zero-allocation player name tab-completion, instant boot sync, and server-switch consistency.
 */
public final class NetworkPresenceManager {

    private final ZelChatCross plugin;
    private final Map<UUID, NetworkPlayer> playersByUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> uuidByName = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> playersByServer = new ConcurrentHashMap<>();
    private final Map<String, Long> serverHeartbeats = new ConcurrentHashMap<>();
    private final Map<String, String> serverDisplayNames = new ConcurrentHashMap<>();

    private volatile List<String> cachedPlayerNames = Collections.emptyList();
    private TaskHandle heartbeatTask;
    private TaskHandle cleanupTask;

    public NetworkPresenceManager(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int interval = plugin.getConfigManager().getPresenceHeartbeatInterval();

        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplay = plugin.getConfigManager().getServerDisplayName();
        serverDisplayNames.put(serverId, serverDisplay);
        serverHeartbeats.put(serverId, System.currentTimeMillis());
        playersByServer.computeIfAbsent(serverId, k -> ConcurrentHashMap.newKeySet());

        // Register initial local players
        for (Player p : Bukkit.getOnlinePlayers()) {
            registerLocalPlayer(p);
        }
        updateCachedPlayerNames();

        // Send initial heartbeat and broadcast sync request to all other servers
        sendHeartbeat();
        requestNetworkSync();

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
        cachedPlayerNames = Collections.emptyList();
    }

    public void requestNetworkSync() {
        String serverId = plugin.getConfigManager().getServerId();
        PresencePayload syncRequest = new PresencePayload(
                serverId,
                plugin.getConfigManager().getServerDisplayName(),
                PresencePayload.Type.SYNC_REQUEST,
                null,
                null,
                null
        );
        plugin.getRedisManager().getPublisher().publishPresence(syncRequest);
    }

    public void onLocalPlayerJoin(Player player) {
        registerLocalPlayer(player);
        updateCachedPlayerNames();

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

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[Debug] Published PLAYER_JOIN for " + player.getName() + " on " + serverId);
        }
    }

    public void onLocalPlayerQuit(Player player) {
        unregisterLocalPlayer(player.getUniqueId(), player.getName());
        updateCachedPlayerNames();

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

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[Debug] Published PLAYER_QUIT for " + player.getName() + " on " + serverId);
        }
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

    public void sendHeartbeat() {
        try {
            String serverId = plugin.getConfigManager().getServerId();
            String serverDisplay = plugin.getConfigManager().getServerDisplayName();

            Map<String, String> localPlayers = new HashMap<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                localPlayers.put(p.getUniqueId().toString(), p.getName());
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
        String localServer = plugin.getConfigManager().getServerId();

        if (originServer.equalsIgnoreCase(localServer)) {
            plugin.getLogger().warning("[Presence] WARNING: Detected another server sending presence with identical server-id '"
                    + localServer + "'. Please configure a unique server-id in config.yml for each server!");
            return;
        }

        String serverDisplay = payload.getServerDisplayName() != null ? payload.getServerDisplayName() : originServer;
        serverDisplayNames.put(originServer, serverDisplay);
        serverHeartbeats.put(originServer, System.currentTimeMillis());

        switch (payload.getType()) {
            case SYNC_REQUEST -> {
                // Reply immediately with local heartbeat so the newly started server gets our players right away
                sendHeartbeat();
            }
            case PLAYER_JOIN -> {
                if (payload.getSinglePlayerUuid() != null && payload.getSinglePlayerName() != null) {
                    UUID uuid = payload.getSinglePlayerUuid();
                    // Remove from all other server buckets to handle server switches
                    for (Map.Entry<String, Set<UUID>> entry : playersByServer.entrySet()) {
                        if (!entry.getKey().equals(originServer)) {
                            entry.getValue().remove(uuid);
                        }
                    }

                    NetworkPlayer netPlayer = new NetworkPlayer(
                            uuid,
                            payload.getSinglePlayerName(),
                            originServer,
                            serverDisplay
                    );
                    playersByUuid.put(uuid, netPlayer);
                    uuidByName.put(payload.getSinglePlayerName().toLowerCase(Locale.ROOT), uuid);
                    playersByServer.computeIfAbsent(originServer, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                    updateCachedPlayerNames();

                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("[Debug] Network presence registered: " + payload.getSinglePlayerName() + " on " + originServer);
                    }
                }
            }
            case PLAYER_QUIT -> {
                if (payload.getSinglePlayerUuid() != null) {
                    UUID uuid = payload.getSinglePlayerUuid();
                    NetworkPlayer current = playersByUuid.get(uuid);
                    if (current != null && current.getServerId().equals(originServer)) {
                        playersByUuid.remove(uuid);
                        if (payload.getSinglePlayerName() != null) {
                            uuidByName.remove(payload.getSinglePlayerName().toLowerCase(Locale.ROOT));
                        }
                    }
                    Set<UUID> set = playersByServer.get(originServer);
                    if (set != null) {
                        set.remove(uuid);
                    }
                    updateCachedPlayerNames();

                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().info("[Debug] Network presence unregistered: " + payload.getSinglePlayerName() + " on " + originServer);
                    }
                }
            }
            case SERVER_HEARTBEAT -> {
                Map<String, String> remotePlayers = payload.getOnlinePlayers();
                Set<UUID> currentServerUuids = playersByServer.computeIfAbsent(originServer, k -> ConcurrentHashMap.newKeySet());

                Set<UUID> remoteUuids = new HashSet<>();
                if (remotePlayers != null) {
                    for (String uuidStr : remotePlayers.keySet()) {
                        try {
                            remoteUuids.add(UUID.fromString(uuidStr));
                        } catch (Exception ignored) {}
                    }
                }

                // Remove players no longer present on that server only if their current registered server is originServer
                Set<UUID> toRemove = new HashSet<>();
                for (UUID u : currentServerUuids) {
                    if (!remoteUuids.contains(u)) {
                        toRemove.add(u);
                    }
                }
                for (UUID u : toRemove) {
                    NetworkPlayer current = playersByUuid.get(u);
                    if (current != null && current.getServerId().equals(originServer)) {
                        playersByUuid.remove(u);
                        uuidByName.remove(current.getUsername().toLowerCase(Locale.ROOT));
                        currentServerUuids.remove(u);
                    }
                }

                // Add or update current players
                if (remotePlayers != null) {
                    for (Map.Entry<String, String> entry : remotePlayers.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            String name = entry.getValue();
                            NetworkPlayer netPlayer = new NetworkPlayer(uuid, name, originServer, serverDisplay);
                            playersByUuid.put(uuid, netPlayer);
                            uuidByName.put(name.toLowerCase(Locale.ROOT), uuid);
                            currentServerUuids.add(uuid);
                        } catch (Exception ignored) {}
                    }
                }
                updateCachedPlayerNames();

                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("[Debug] Updated heartbeat from " + originServer + " (" + remoteUuids.size() + " players)");
                }
            }
            case SERVER_STOP -> {
                removeServer(originServer);
                updateCachedPlayerNames();
            }
        }
    }

    private void updateCachedPlayerNames() {
        List<String> names = new ArrayList<>(playersByUuid.size());
        for (NetworkPlayer np : playersByUuid.values()) {
            names.add(np.getUsername());
        }
        this.cachedPlayerNames = Collections.unmodifiableList(names);
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

        if (!deadServers.isEmpty()) {
            for (String deadServer : deadServers) {
                plugin.getLogger().warning("[Presence] Server '" + deadServer + "' timed out. Removing cached players.");
                removeServer(deadServer);
            }
            updateCachedPlayerNames();
        }
    }

    private void removeServer(String serverId) {
        serverHeartbeats.remove(serverId);
        serverDisplayNames.remove(serverId);
        Set<UUID> players = playersByServer.remove(serverId);
        if (players != null) {
            for (UUID uuid : players) {
                NetworkPlayer current = playersByUuid.get(uuid);
                if (current != null && current.getServerId().equals(serverId)) {
                    playersByUuid.remove(uuid);
                    uuidByName.remove(current.getUsername().toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public NetworkPlayer getPlayerByName(String name) {
        if (name == null || name.isEmpty()) return null;
        UUID uuid = uuidByName.get(name.toLowerCase(Locale.ROOT));
        if (uuid != null) {
            NetworkPlayer player = playersByUuid.get(uuid);
            if (player != null) return player;
        }
        // Fallback case-insensitive scan
        for (NetworkPlayer p : playersByUuid.values()) {
            if (p.getUsername().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public NetworkPlayer getPlayerByUuid(UUID uuid) {
        if (uuid == null) return null;
        return playersByUuid.get(uuid);
    }

    public Collection<NetworkPlayer> getAllOnlinePlayers() {
        return Collections.unmodifiableCollection(playersByUuid.values());
    }

    public List<String> getOnlinePlayerNames() {
        return cachedPlayerNames;
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
