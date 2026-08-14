package dev.lyy.zelchatcross.chat;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.redis.payload.ChatMessagePayload;
import dev.lyy.zelchatcross.redis.payload.StaffChatPayload;
import it.pino.zelchat.api.ZelChatAPI;
import it.pino.zelchat.api.player.ChatPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles incoming cross-server chat messages and distributes them locally.
 */
public final class CrossChatManager {

    private final ZelChatCross plugin;
    private final Map<String, Long> recentlyPublished = new ConcurrentHashMap<>();

    public CrossChatManager(ZelChatCross plugin) {
        this.plugin = plugin;
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

        int deliveredCount = 0;
        // Distribute to local players while respecting ZelChat ignore lists
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isIgnoring(player.getUniqueId(), senderUuid)) {
                continue;
            }
            player.sendMessage(finalMessage);
            deliveredCount++;
        }

        // Also log to console
        Bukkit.getConsoleSender().sendMessage(finalMessage);

        if (cfg.isDebug()) {
            plugin.getLogger().info("[Debug] Delivered cross-server chat from "
                    + payload.getSenderName() + "@" + payload.getOriginServerId()
                    + " to " + deliveredCount + " local players.");
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

    /**
     * Checks if a local player is ignoring the sender via ZelChat API.
     */
    public boolean isIgnoring(UUID localPlayerUuid, UUID senderUuid) {
        try {
            if (ZelChatAPI.get() != null && ZelChatAPI.get().getPlayerService() != null) {
                ChatPlayer chatPlayer = ZelChatAPI.get().getPlayerService().getOnlinePlayers().get(localPlayerUuid);
                if (chatPlayer != null && chatPlayer.getHiddenPlayers() != null) {
                    return chatPlayer.getHiddenPlayers().contains(senderUuid);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
