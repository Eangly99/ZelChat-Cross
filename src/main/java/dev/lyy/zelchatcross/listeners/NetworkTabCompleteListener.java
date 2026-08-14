package dev.lyy.zelchatcross.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Provides instant network-wide tab-completion for cross-server private messaging
 * across all connected servers (Paper & Folia).
 */
public final class NetworkTabCompleteListener implements Listener {

    private static final Set<String> MSG_COMMANDS = Set.of(
            "msg", "tell", "w", "whisper", "pm", "m"
    );

    private final ZelChatCross plugin;

    public NetworkTabCompleteListener(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        if (!plugin.getConfigManager().isPresenceTabComplete()) {
            return;
        }

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) {
            return;
        }

        String withoutSlash = buffer.substring(1);
        String[] parts = withoutSlash.split(" ", -1);

        if (parts.length == 0) {
            return;
        }

        String cmd = parts[0].toLowerCase(Locale.ROOT);
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        if (MSG_COMMANDS.contains(cmd) && parts.length == 2) {
            String prefix = parts[1].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            String senderName = event.getSender().getName();

            for (String name : plugin.getPresenceManager().getOnlinePlayerNames()) {
                if (name.equalsIgnoreCase(senderName)) continue;
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    completions.add(name);
                }
            }
            event.setCompletions(completions);
            event.setHandled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSyncTabComplete(TabCompleteEvent event) {
        if (!plugin.getConfigManager().isPresenceTabComplete()) {
            return;
        }

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) {
            return;
        }

        String withoutSlash = buffer.substring(1);
        String[] parts = withoutSlash.split(" ", -1);

        if (parts.length == 0) {
            return;
        }

        String cmd = parts[0].toLowerCase(Locale.ROOT);
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        if (MSG_COMMANDS.contains(cmd) && parts.length == 2) {
            String prefix = parts[1].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            String senderName = event.getSender().getName();

            for (String name : plugin.getPresenceManager().getOnlinePlayerNames()) {
                if (name.equalsIgnoreCase(senderName)) continue;
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    completions.add(name);
                }
            }
            event.setCompletions(completions);
        }
    }
}
