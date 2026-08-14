package dev.lyy.zelchatcross.listeners;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.ChatMessagePayload;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

/**
 * Fallback listener capturing standard Paper/Spigot chat events to guarantee network sync
 * when ZelChat module pipeline is not active or passes through to Bukkit.
 */
public final class PaperChatListener implements Listener {

    private final ZelChatCross plugin;

    public PaperChatListener(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPaperChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());

        handleChatEvent(player, plain);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpigotChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        handleChatEvent(player, message);
    }

    private void handleChatEvent(Player player, String rawMessage) {
        if (!plugin.getConfigManager().isSyncPublic()) {
            return;
        }

        // Deduplication: if ZelCrossChatModule already handled this exact message
        if (plugin.getChatManager().isRecentlyPublished(player.getUniqueId(), rawMessage)) {
            return;
        }

        if (plugin.getModerationManager().isChatMuted() && !player.hasPermission("zelcross.admin.chatmute.bypass")) {
            return;
        }

        // Mark as published so neither listener nor module duplicate it
        plugin.getChatManager().markPublished(player.getUniqueId(), rawMessage);

        String sanitized = player.hasPermission("zelcross.chat.format") ? rawMessage :
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(rawMessage);

        String processedRaw = plugin.getShowcaseManager().processChatShowcases(player, sanitized);
        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplayName = plugin.getConfigManager().getServerDisplayName();

        ChatMessagePayload payload = new ChatMessagePayload(
                serverId,
                player.getUniqueId(),
                player.getName(),
                serverDisplayName,
                "EVERYONE",
                "global",
                rawMessage,
                processedRaw,
                List.of()
        );

        plugin.getRedisManager().getPublisher().publishChat(payload);

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[Debug] Published chat via PaperChatListener from "
                    + player.getName() + " on " + serverId + ": " + rawMessage);
        }
    }
}
