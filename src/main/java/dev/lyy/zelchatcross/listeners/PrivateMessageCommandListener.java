package dev.lyy.zelchatcross.listeners;

import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Intercepts private message commands (/msg, /tell, /w, /whisper, /pm, /r, /reply)
 * before other plugins (ZelChat local commands, Essentials, Vanilla) consume them.
 * Guarantees cross-server routing across the entire network.
 */
public final class PrivateMessageCommandListener implements Listener {

    private static final Set<String> MSG_COMMANDS = Set.of(
            "msg", "tell", "w", "whisper", "pm", "m"
    );

    private static final Set<String> REPLY_COMMANDS = Set.of(
            "r", "reply"
    );

    private final ZelChatCross plugin;

    public PrivateMessageCommandListener(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfigManager().isPmEnabled()) {
            return;
        }

        String raw = event.getMessage();
        if (!raw.startsWith("/")) {
            return;
        }

        String messageWithoutSlash = raw.substring(1).trim();
        if (messageWithoutSlash.isEmpty()) {
            return;
        }

        String[] parts = messageWithoutSlash.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String cmdLabel = parts[0].toLowerCase(Locale.ROOT);
        // Strip namespace if present (e.g. /minecraft:msg -> msg, /essentials:msg -> msg)
        if (cmdLabel.contains(":")) {
            cmdLabel = cmdLabel.substring(cmdLabel.indexOf(':') + 1);
        }

        Player player = event.getPlayer();

        if (MSG_COMMANDS.contains(cmdLabel)) {
            event.setCancelled(true);

            if (!player.hasPermission("zelcross.msg")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return;
            }

            if (parts.length < 3) {
                player.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + parts[0] + " <player> <message></red>"));
                return;
            }

            String targetName = parts[1];
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                if (i > 2) msgBuilder.append(" ");
                msgBuilder.append(parts[i]);
            }
            String message = msgBuilder.toString().trim();

            if (message.isEmpty()) {
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-send-empty"));
                return;
            }

            plugin.getPrivateMessageManager().sendPrivateMessage(player, targetName, message);
        } else if (REPLY_COMMANDS.contains(cmdLabel)) {
            event.setCancelled(true);

            if (!player.hasPermission("zelcross.reply")) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return;
            }

            if (parts.length < 2) {
                player.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + parts[0] + " <message></red>"));
                return;
            }

            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (i > 1) msgBuilder.append(" ");
                msgBuilder.append(parts[i]);
            }
            String message = msgBuilder.toString().trim();

            if (message.isEmpty()) {
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-send-empty"));
                return;
            }

            plugin.getPrivateMessageManager().sendReply(player, message);
        }
    }
}
