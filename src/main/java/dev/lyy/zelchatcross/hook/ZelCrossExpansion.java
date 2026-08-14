package dev.lyy.zelchatcross.hook;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.presence.NetworkPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for ZelChat-Cross placeholders.
 */
public final class ZelCrossExpansion extends PlaceholderExpansion {

    private final ZelChatCross plugin;

    public ZelCrossExpansion(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zelcross";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lyy";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase();

        switch (lower) {
            case "network_online" -> {
                return String.valueOf(plugin.getPresenceManager().getNetworkOnlineCount());
            }
            case "server" -> {
                return plugin.getConfigManager().getServerId();
            }
            case "server_display" -> {
                return plugin.getConfigManager().getServerDisplayName();
            }
            case "server_count" -> {
                return String.valueOf(plugin.getPresenceManager().getServerPlayerCounts().size());
            }
            case "is_muted" -> {
                return String.valueOf(plugin.getModerationManager().isChatMuted());
            }
        }

        if (lower.startsWith("player_server_")) {
            String targetName = params.substring("player_server_".length());
            NetworkPlayer netPlayer = plugin.getPresenceManager().getPlayerByName(targetName);
            if (netPlayer != null) {
                return netPlayer.getServerId();
            }
            return "offline";
        }

        if (player != null && player.getName() != null) {
            if (lower.equals("player_server")) {
                NetworkPlayer netPlayer = plugin.getPresenceManager().getPlayerByUuid(player.getUniqueId());
                if (netPlayer != null) {
                    return netPlayer.getServerId();
                }
            }
        }

        return null;
    }
}
