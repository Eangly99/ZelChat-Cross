package dev.lyy.zelchatcross.messaging;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.presence.NetworkPlayer;
import dev.lyy.zelchatcross.redis.payload.PrivateMessagePayload;
import dev.lyy.zelchatcross.redis.payload.SocialSpyPayload;
import it.pino.zelchat.api.ZelChatAPI;
import it.pino.zelchat.api.player.ChatPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cross-server private messaging (/msg, /tell, /reply) and SocialSpy.
 */
public final class PrivateMessageManager {

    private final ZelChatCross plugin;
    private final Map<UUID, UUID> lastReplierUuid = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastReplierName = new ConcurrentHashMap<>();
    private final Set<UUID> spyEnabledPlayers = ConcurrentHashMap.newKeySet();

    public PrivateMessageManager(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    public boolean toggleSpy(Player player) {
        if (spyEnabledPlayers.contains(player.getUniqueId())) {
            spyEnabledPlayers.remove(player.getUniqueId());
            return false;
        } else {
            spyEnabledPlayers.add(player.getUniqueId());
            return true;
        }
    }

    public boolean isSpyEnabled(Player player) {
        return spyEnabledPlayers.contains(player.getUniqueId());
    }

    public void sendPrivateMessage(Player sender, String targetName, String message) {
        ConfigManager cfg = plugin.getConfigManager();

        if (sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(cfg.getMessage("cannot-message-self"));
            return;
        }

        NetworkPlayer target = plugin.getPresenceManager().getPlayerByName(targetName);
        if (target == null) {
            sender.sendMessage(cfg.getMessage("player-not-found", Map.of("player", targetName)));
            return;
        }

        // Check local player PM toggle / ignore if target is on the same server
        Player localTarget = Bukkit.getPlayer(target.getUniqueId());
        if (localTarget != null) {
            if (isIgnoring(localTarget.getUniqueId(), sender.getUniqueId()) && !sender.hasPermission("zelcross.admin")) {
                sender.sendMessage(cfg.getMessage("target-ignoring-you", Map.of("target", target.getUsername())));
                return;
            }
            if (hasClosedMessages(localTarget.getUniqueId()) && !sender.hasPermission("zelcross.admin")) {
                sender.sendMessage(cfg.getMessage("target-pm-disabled", Map.of("target", target.getUsername())));
                return;
            }
        }

        // Process showcase placeholders in private message as well
        String processedMessage = plugin.getShowcaseManager().processChatShowcases(sender, message);

        String currentServerId = cfg.getServerId();
        String currentServerDisplay = cfg.getServerDisplayName();
        String targetServerDisplay = target.getServerDisplayName();

        // Sender View
        String sendFormat = cfg.getPmFormatSend()
                .replace("<target>", target.getUsername())
                .replace("<target_server>", target.getServerId())
                .replace("<target_server_display>", targetServerDisplay)
                .replace("<message>", processedMessage);
        sender.sendMessage(cfg.parse(sendFormat));

        // Update sender's replier memory
        lastReplierUuid.put(sender.getUniqueId(), target.getUniqueId());
        lastReplierName.put(sender.getUniqueId(), target.getUsername());

        // Deliver to target
        if (localTarget != null && localTarget.isOnline()) {
            // Local delivery
            String receiveFormat = cfg.getPmFormatReceive()
                    .replace("<sender>", sender.getName())
                    .replace("<sender_server>", currentServerId)
                    .replace("<sender_server_display>", currentServerDisplay)
                    .replace("<message>", processedMessage);
            localTarget.sendMessage(cfg.parse(receiveFormat));

            // Update local recipient's replier memory
            lastReplierUuid.put(localTarget.getUniqueId(), sender.getUniqueId());
            lastReplierName.put(localTarget.getUniqueId(), sender.getName());
        } else {
            // Remote delivery via Redis
            PrivateMessagePayload payload = new PrivateMessagePayload(
                    currentServerId,
                    sender.getUniqueId(),
                    sender.getName(),
                    currentServerDisplay,
                    target.getUniqueId(),
                    target.getUsername(),
                    processedMessage
            );
            plugin.getRedisManager().getPublisher().publishPrivateMessage(payload);
        }

        // Dispatch SocialSpy
        SocialSpyPayload spyPayload = new SocialSpyPayload(
                currentServerId,
                sender.getUniqueId(),
                sender.getName(),
                currentServerDisplay,
                target.getUniqueId(),
                target.getUsername(),
                targetServerDisplay,
                processedMessage
        );
        // Render to local spies
        renderSpyLocally(spyPayload);
        // Publish to remote spies
        plugin.getRedisManager().getPublisher().publishSpy(spyPayload);
    }

    public void sendReply(Player sender, String message) {
        ConfigManager cfg = plugin.getConfigManager();
        UUID replierId = lastReplierUuid.get(sender.getUniqueId());
        String replierName = lastReplierName.get(sender.getUniqueId());

        if (replierId == null && replierName == null) {
            sender.sendMessage(cfg.getMessage("no-reply-target"));
            return;
        }

        NetworkPlayer target = replierId != null ? plugin.getPresenceManager().getPlayerByUuid(replierId) : null;
        if (target == null && replierName != null) {
            target = plugin.getPresenceManager().getPlayerByName(replierName);
        }

        if (target == null) {
            String name = replierName != null ? replierName : "Unknown";
            sender.sendMessage(cfg.getMessage("player-not-found", Map.of("player", name)));
            return;
        }

        sendPrivateMessage(sender, target.getUsername(), message);
    }

    public void handleIncomingPrivateMessage(PrivateMessagePayload payload) {
        UUID targetUuid = payload.getTargetUuid();
        Player localTarget = targetUuid != null ? Bukkit.getPlayer(targetUuid) : Bukkit.getPlayerExact(payload.getTargetName());

        if (localTarget == null || !localTarget.isOnline()) {
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();

        // Check if local target is ignoring the sender
        if (isIgnoring(localTarget.getUniqueId(), payload.getSenderUuid())) {
            return;
        }
        if (hasClosedMessages(localTarget.getUniqueId())) {
            return;
        }

        // Format recipient message
        String receiveFormat = cfg.getPmFormatReceive()
                .replace("<sender>", payload.getSenderName())
                .replace("<sender_server>", payload.getOriginServerId())
                .replace("<sender_server_display>", payload.getSenderServerDisplayName())
                .replace("<message>", payload.getMessage());

        localTarget.sendMessage(cfg.parse(receiveFormat));

        // Update recipient's replier target
        lastReplierUuid.put(localTarget.getUniqueId(), payload.getSenderUuid());
        lastReplierName.put(localTarget.getUniqueId(), payload.getSenderName());
    }

    public void handleIncomingSpyMessage(SocialSpyPayload payload) {
        renderSpyLocally(payload);
    }

    private void renderSpyLocally(SocialSpyPayload payload) {
        ConfigManager cfg = plugin.getConfigManager();

        String spyFormat = cfg.getPmFormatSpy()
                .replace("<sender>", payload.getSenderName())
                .replace("<sender_server>", payload.getOriginServerId())
                .replace("<sender_server_display>", payload.getSenderServerDisplayName())
                .replace("<target>", payload.getTargetName())
                .replace("<target_server_display>", payload.getTargetServerDisplayName())
                .replace("<message>", payload.getMessage());

        Component spyComponent = cfg.parse(spyFormat);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("zelcross.admin.spy") && spyEnabledPlayers.contains(player.getUniqueId())) {
                // Don't send spy message to the sender or receiver
                if (!player.getUniqueId().equals(payload.getSenderUuid()) &&
                        !player.getUniqueId().equals(payload.getTargetUuid())) {
                    player.sendMessage(spyComponent);
                }
            }
        }
    }

    private boolean isIgnoring(UUID localPlayerUuid, UUID senderUuid) {
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

    private boolean hasClosedMessages(UUID localPlayerUuid) {
        try {
            if (ZelChatAPI.get() != null && ZelChatAPI.get().getPlayerService() != null) {
                ChatPlayer chatPlayer = ZelChatAPI.get().getPlayerService().getOnlinePlayers().get(localPlayerUuid);
                if (chatPlayer != null) {
                    return chatPlayer.hasMessagesClosed();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
