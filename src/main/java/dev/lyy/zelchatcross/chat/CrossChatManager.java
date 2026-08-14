package dev.lyy.zelchatcross.chat;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.redis.payload.ChatMessagePayload;
import dev.lyy.zelchatcross.redis.payload.StaffChatPayload;
import dev.lyy.zelchatcross.scheduler.TaskHandle;
import it.pino.zelchat.api.ZelChatAPI;
import it.pino.zelchat.api.player.ChatPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles incoming cross-server chat messages and distributes them locally with high-performance
 * audience dispatching and sliding-window deduplication.
 */
public final class CrossChatManager {

    private final ZelChatCross plugin;
    private final Map<String, Long> recentlyPublished = new ConcurrentHashMap<>();
    private TaskHandle cleanupTask;

    public CrossChatManager(ZelChatCross plugin) {
        this.plugin = plugin;
        this.cleanupTask = plugin.getScheduler().runAsyncRepeating(
                this::cleanupDeduplicationCache,
                30,
                30,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        recentlyPublished.clear();
    }

    public void markPublished(UUID playerUuid, String message) {
        if (playerUuid != null && message != null) {
            recentlyPublished.put(playerUuid + ":" + message.trim(), System.currentTimeMillis() + 1500L);
        }
    }

    public boolean isRecentlyPublished(UUID playerUuid, String message) {
        if (playerUuid == null || message == null) return false;
        Long expiry = recentlyPublished.get(playerUuid + ":" + message.trim());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            recentlyPublished.remove(playerUuid + ":" + message.trim());
            return false;
        }
        return true;
    }

    private void cleanupDeduplicationCache() {
        long now = System.currentTimeMillis();
        recentlyPublished.entrySet().removeIf(entry -> now > entry.getValue());
    }

    /**
     * Handles an incoming public/custom chat message from a remote server.
     */
    public void handleIncomingChat(ChatMessagePayload payload) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isSyncPublic()) {
            return;
        }

        UUID senderUuid = payload.getSenderUuid();
        String originServerDisplay = payload.getOriginServerDisplayName() != null ?
                payload.getOriginServerDisplayName() : payload.getOriginServerId();

        // Parse content
        String prefixFormat = cfg.getPublicIncomingPrefix()
                .replace("<origin_server>", payload.getOriginServerId())
                .replace("<origin_server_display>", originServerDisplay)
                .replace("<sender>", payload.getSenderName());

        Component prefixComponent = cfg.parse(prefixFormat);
        Component bodyComponent = cfg.parse(payload.getMiniMessageContent());
        Component finalMessage = prefixComponent.append(bodyComponent);

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage(finalMessage);
            return;
        }

        // Fast-path: Check if any local players are ignoring this sender
        Set<UUID> ignoringPlayers = getIgnoringPlayers(senderUuid, online);

        if (ignoringPlayers.isEmpty()) {
            // Direct audience broadcast - avoids per-player map lookups
            for (Player player : online) {
                player.sendMessage(finalMessage);
            }
        } else {
            // Filtered delivery for players who are not ignoring
            for (Player player : online) {
                if (!ignoringPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(finalMessage);
                }
            }
        }

        // Log to console
        Bukkit.getConsoleSender().sendMessage(finalMessage);

        if (cfg.isDebug()) {
            plugin.getLogger().info("[Debug] Delivered cross-server chat from "
                    + payload.getSenderName() + "@" + payload.getOriginServerId()
                    + " to " + (online.size() - ignoringPlayers.size()) + " local players.");
        }
    }

    /**
     * Handles an incoming staff chat message from a remote server.
     */
    public void handleIncomingStaffChat(StaffChatPayload payload) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.isSyncStaff()) {
            return;
        }

        String originServerDisplay = payload.getOriginServerDisplayName() != null ?
                payload.getOriginServerDisplayName() : payload.getOriginServerId();

        String prefixFormat = cfg.getStaffIncomingPrefix()
                .replace("<origin_server>", payload.getOriginServerId())
                .replace("<origin_server_display>", originServerDisplay)
                .replace("<sender>", payload.getSenderName());

        Component prefixComponent = cfg.parse(prefixFormat);
        Component bodyComponent = cfg.parse(payload.getMessage());
        Component finalMessage = prefixComponent.append(bodyComponent);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("zelcross.staff.chat")) {
                player.sendMessage(finalMessage);
            }
        }
        Bukkit.getConsoleSender().sendMessage(finalMessage);

        if (cfg.isDebug()) {
            plugin.getLogger().info("[Debug] Delivered cross-server staff chat from "
                    + payload.getSenderName() + "@" + payload.getOriginServerId());
        }
    }

    private Set<UUID> getIgnoringPlayers(UUID senderUuid, Collection<? extends Player> online) {
        if (senderUuid == null) return Collections.emptySet();
        Set<UUID> ignoring = null;
        try {
            if (ZelChatAPI.get() != null && ZelChatAPI.get().getPlayerService() != null) {
                Map<UUID, ChatPlayer> map = ZelChatAPI.get().getPlayerService().getOnlinePlayers();
                for (Player player : online) {
                    ChatPlayer chatPlayer = map.get(player.getUniqueId());
                    if (chatPlayer != null && chatPlayer.getHiddenPlayers() != null && chatPlayer.getHiddenPlayers().contains(senderUuid)) {
                        if (ignoring == null) ignoring = new HashSet<>();
                        ignoring.add(player.getUniqueId());
                    }
                }
            }
        } catch (Exception ignored) {}
        return ignoring != null ? ignoring : Collections.emptySet();
    }
}
