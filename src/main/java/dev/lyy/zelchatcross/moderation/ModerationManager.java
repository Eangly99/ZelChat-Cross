package dev.lyy.zelchatcross.moderation;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.redis.payload.ModerationPayload;
import it.pino.zelchat.api.ZelChatAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles network-wide moderation actions (global mute, broadcasts, clear chat).
 */
public final class ModerationManager {

    private final ZelChatCross plugin;
    private volatile boolean globalChatMuted = false;

    public ModerationManager(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    public boolean isChatMuted() {
        return globalChatMuted;
    }

    public void setGlobalChatMuted(String senderName, boolean muted) {
        this.globalChatMuted = muted;
        String serverId = plugin.getConfigManager().getServerId();

        // Sync with local ZelChat API
        syncLocalZelChatMute(muted);

        ModerationPayload payload = new ModerationPayload(
                serverId,
                muted ? ModerationPayload.Action.CHAT_MUTE : ModerationPayload.Action.CHAT_UNMUTE,
                senderName,
                null,
                null,
                0,
                0
        );
        plugin.getRedisManager().getPublisher().publishModeration(payload);

        // Notify local server
        Component msg = muted ?
                plugin.getConfigManager().getMessage("chat-muted-globally", Map.of("sender", senderName)) :
                plugin.getConfigManager().getMessage("chat-unmuted-globally", Map.of("sender", senderName));
        Bukkit.broadcast(msg);
    }

    public void broadcastGlobal(String senderName, String message) {
        String serverId = plugin.getConfigManager().getServerId();
        ConfigManager cfg = plugin.getConfigManager();

        ModerationPayload payload = new ModerationPayload(
                serverId,
                ModerationPayload.Action.BROADCAST,
                senderName,
                message,
                cfg.getBroadcastSound(),
                cfg.getBroadcastSoundVolume(),
                cfg.getBroadcastSoundPitch()
        );
        plugin.getRedisManager().getPublisher().publishModeration(payload);

        // Render locally
        renderBroadcast(payload);
    }

    public void clearGlobalChat(String senderName) {
        String serverId = plugin.getConfigManager().getServerId();
        ModerationPayload payload = new ModerationPayload(
                serverId,
                ModerationPayload.Action.CLEAR_CHAT,
                senderName,
                null,
                null,
                0,
                0
        );
        plugin.getRedisManager().getPublisher().publishModeration(payload);

        // Clear locally
        renderClearChat(payload);
    }

    public void handleIncomingModeration(ModerationPayload payload) {
        switch (payload.getAction()) {
            case CHAT_MUTE -> {
                this.globalChatMuted = true;
                syncLocalZelChatMute(true);
                Component msg = plugin.getConfigManager().getMessage("chat-muted-globally",
                        Map.of("sender", payload.getSenderName()));
                Bukkit.broadcast(msg);
            }
            case CHAT_UNMUTE -> {
                this.globalChatMuted = false;
                syncLocalZelChatMute(false);
                Component msg = plugin.getConfigManager().getMessage("chat-unmuted-globally",
                        Map.of("sender", payload.getSenderName()));
                Bukkit.broadcast(msg);
            }
            case BROADCAST -> {
                renderBroadcast(payload);
            }
            case CLEAR_CHAT -> {
                renderClearChat(payload);
            }
        }
    }

    private void renderBroadcast(ModerationPayload payload) {
        ConfigManager cfg = plugin.getConfigManager();
        Component prefix = cfg.parse(cfg.getBroadcastPrefix());
        Component body = cfg.parse(payload.getContent());
        Component fullBroadcast = prefix.append(body);

        Sound sound = null;
        if (payload.getSound() != null && !payload.getSound().isEmpty()) {
            try {
                String soundKey = payload.getSound().toLowerCase(Locale.ROOT).replace('_', '.');
                if (!soundKey.contains(":")) {
                    soundKey = "minecraft:" + soundKey;
                }
                sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, payload.getSoundVolume(), payload.getSoundPitch());
            } catch (Exception ignored) {}
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(fullBroadcast);
            if (sound != null) {
                player.playSound(sound);
            }
        }
        Bukkit.getConsoleSender().sendMessage(fullBroadcast);
    }

    private void renderClearChat(ModerationPayload payload) {
        Component blank = Component.text(" ");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("zelcross.admin.clearchat.bypass")) {
                for (int i = 0; i < 100; i++) {
                    player.sendMessage(blank);
                }
            }
        }
        Component alert = plugin.getConfigManager().getMessage("chat-cleared-globally",
                Map.of("sender", payload.getSenderName()));
        Bukkit.broadcast(alert);
    }

    private void syncLocalZelChatMute(boolean muted) {
        try {
            if (ZelChatAPI.get() != null) {
                ZelChatAPI.get().setChatMuted(muted);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[Moderation] Failed to sync local ZelChat mute: " + e.getMessage());
        }
    }
}
