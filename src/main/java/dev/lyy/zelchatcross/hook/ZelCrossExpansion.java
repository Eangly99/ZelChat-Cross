package dev.lyy.zelchatcross.hook;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.presence.NetworkPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        String lower = params.toLowerCase(Locale.ROOT);

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

        // %zelcross_player_server_display_<player>%
        if (lower.startsWith("player_server_display_")) {
            String targetName = params.substring("player_server_display_".length());
            NetworkPlayer netPlayer = plugin.getPresenceManager().getPlayerByName(targetName);
            if (netPlayer != null) {
                return netPlayer.getServerDisplayName();
            }
            return "offline";
        }

        // %zelcross_player_server_<player>%
        if (lower.startsWith("player_server_")) {
            String targetName = params.substring("player_server_".length());
            NetworkPlayer netPlayer = plugin.getPresenceManager().getPlayerByName(targetName);
            if (netPlayer != null) {
                return netPlayer.getServerId();
            }
            return "offline";
        }

        // %zelcross_server_players_<server>%
        if (lower.startsWith("server_players_")) {
            String serverName = params.substring("server_players_".length());
            Map<String, Integer> counts = plugin.getPresenceManager().getServerPlayerCounts();
            return String.valueOf(counts.getOrDefault(serverName, 0));
        }

        // Contextual player placeholders
        if (player != null) {
            if (lower.equals("player_server")) {
                NetworkPlayer netPlayer = plugin.getPresenceManager().getPlayerByUuid(player.getUniqueId());
                if (netPlayer != null) {
                    return netPlayer.getServerId();
                }
                if (player.getName() != null) {
                    netPlayer = plugin.getPresenceManager().getPlayerByName(player.getName());
                    if (netPlayer != null) {
                        return netPlayer.getServerId();
                    }
                }
                return plugin.getConfigManager().getServerId();
            }

            if (lower.equals("player_server_display")) {
                NetworkPlayer netPlayer = plugin.getPresenceManager().getPlayerByUuid(player.getUniqueId());
                if (netPlayer != null) {
                    return netPlayer.getServerDisplayName();
                }
                if (player.getName() != null) {
                    netPlayer = plugin.getPresenceManager().getPlayerByName(player.getName());
                    if (netPlayer != null) {
                        return netPlayer.getServerDisplayName();
                    }
                }
                return plugin.getConfigManager().getServerDisplayName();
            }
        }

        return null;
    }
}
