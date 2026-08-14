package dev.lyy.zelchatcross.listeners;

import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player join/quit events to update network presence.
 */
public final class PlayerConnectionListener implements Listener {

    private final ZelChatCross plugin;

    public PlayerConnectionListener(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPresenceManager().onLocalPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPresenceManager().onLocalPlayerQuit(event.getPlayer());
    }
}
