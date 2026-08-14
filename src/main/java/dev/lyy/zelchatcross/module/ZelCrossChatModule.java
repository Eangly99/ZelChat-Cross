package dev.lyy.zelchatcross.module;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.ChatMessagePayload;
import dev.lyy.zelchatcross.redis.payload.StaffChatPayload;
import it.pino.zelchat.api.message.ChatMessage;
import it.pino.zelchat.api.message.channel.ChannelType;
import it.pino.zelchat.api.message.state.MessageState;
import it.pino.zelchat.api.module.ChatModule;
import it.pino.zelchat.api.module.annotation.ChatModuleSettings;
import it.pino.zelchat.api.module.priority.ModulePriority;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * ZelChat ChatModule expansion for cross-server chat synchronization and item showcasing.
 */
@ChatModuleSettings(pluginOwner = "ZelChat-Cross", priority = ModulePriority.NORMAL)
public final class ZelCrossChatModule implements ChatModule {

    private final ZelChatCross plugin;

    public ZelCrossChatModule(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        plugin.getLogger().info("[Module] ZelCross ChatModule loaded and registered with ZelChat API.");
    }

    @Override
    public void unload() {
        plugin.getLogger().info("[Module] ZelCross ChatModule unloaded.");
    }

    @Override
    public void reload() {
        plugin.getLogger().info("[Module] ZelCross ChatModule reloaded.");
    }

    @Override
    public void handleChatMessage(final @NotNull ChatMessage chatMessage) {
        if (chatMessage.getState() == MessageState.CANCELLED || chatMessage.getState() == MessageState.FILTERED_CANCELLED) {
            return;
        }

        Player player = chatMessage.getBukkitPlayer();
        if (player == null) {
            return;
        }

        // Check global chat mute
        if (plugin.getModerationManager().isChatMuted() && !player.hasPermission("zelcross.admin.chatmute.bypass")) {
            chatMessage.setState(MessageState.CANCELLED);
            player.sendMessage(plugin.getConfigManager().getMessage("chat-is-muted"));
            return;
        }

        String rawMessage = chatMessage.getRawMessage();

        // Check showcase cooldown if showcase token is detected
        if (containsShowcaseToken(rawMessage)) {
            if (plugin.getShowcaseManager().isOnCooldown(player)) {
                long remaining = plugin.getShowcaseManager().getCooldownRemainingSeconds(player);
                player.sendMessage(plugin.getConfigManager().getMessage("showcase-cooldown")
                        .replaceText(b -> b.matchLiteral("{seconds}").replacement(String.valueOf(remaining))));
                chatMessage.setState(MessageState.CANCELLED);
                return;
            }
        }

        // Sanitize raw message if player lacks MiniMessage formatting permission
        String sanitized = player.hasPermission("zelcross.chat.format") ? rawMessage :
                MiniMessage.miniMessage().escapeTags(rawMessage);

        // Process showcase placeholders [item], [inv], [ec]
        String processedRaw = plugin.getShowcaseManager().processChatShowcases(player, sanitized);
        if (!processedRaw.equals(rawMessage)) {
            Component transformed = plugin.getConfigManager().parse(processedRaw);
            chatMessage.setMessage(transformed);
        }

        // Mark as published so PaperChatListener doesn't duplicate
        plugin.getChatManager().markPublished(player.getUniqueId(), rawMessage);

        // Distribute to network via Redis Pub/Sub
        ChannelType type = chatMessage.getChannel().getType();
        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplayName = plugin.getConfigManager().getServerDisplayName();

        if (type == ChannelType.EVERYONE && plugin.getConfigManager().isSyncPublic()) {
            String miniMessageSerialized = MiniMessage.miniMessage().serialize(chatMessage.getMessage());

            ChatMessagePayload payload = new ChatMessagePayload(
                    serverId,
                    player.getUniqueId(),
                    player.getName(),
                    serverDisplayName,
                    "EVERYONE",
                    "global",
                    rawMessage,
                    miniMessageSerialized,
                    chatMessage.getMentions()
            );
            plugin.getRedisManager().getPublisher().publishChat(payload);

            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[Debug] Published chat via ZelCrossChatModule from "
                        + player.getName() + " on " + serverId + ": " + rawMessage);
            }
        } else if (type == ChannelType.STAFF && plugin.getConfigManager().isSyncStaff()) {
            String miniMessageSerialized = MiniMessage.miniMessage().serialize(chatMessage.getMessage());

            StaffChatPayload payload = new StaffChatPayload(
                    serverId,
                    player.getUniqueId(),
                    player.getName(),
                    serverDisplayName,
                    miniMessageSerialized
            );
            plugin.getRedisManager().getPublisher().publishStaffChat(payload);

            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[Debug] Published staff chat via ZelCrossChatModule from "
                        + player.getName() + " on " + serverId);
            }
        }
    }

    private boolean containsShowcaseToken(String message) {
        for (String placeholder : plugin.getConfigManager().getShowcaseItemPlaceholders()) {
            if (message.contains(placeholder)) return true;
        }
        for (String placeholder : plugin.getConfigManager().getShowcaseInvPlaceholders()) {
            if (message.contains(placeholder)) return true;
        }
        for (String placeholder : plugin.getConfigManager().getShowcaseEcPlaceholders()) {
            if (message.contains(placeholder)) return true;
        }
        return false;
    }
}
