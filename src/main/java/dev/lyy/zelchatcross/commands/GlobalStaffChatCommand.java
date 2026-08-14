package dev.lyy.zelchatcross.commands;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.redis.payload.StaffChatPayload;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command for sending a message directly to the network-wide staff chat (/gstaff, /sc).
 */
public final class GlobalStaffChatCommand implements CommandExecutor, TabCompleter {

    private final ZelChatCross plugin;

    public GlobalStaffChatCommand(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelcross.staff.chat")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + label + " <message></red>"));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString().trim();

        if (message.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("cannot-send-empty"));
            return true;
        }

        String serverId = plugin.getConfigManager().getServerId();
        String serverDisplayName = plugin.getConfigManager().getServerDisplayName();

        StaffChatPayload payload = new StaffChatPayload(
                serverId,
                (sender instanceof Player p) ? p.getUniqueId() : null,
                sender.getName(),
                serverDisplayName,
                message
        );

        // Publish to other servers
        plugin.getRedisManager().getPublisher().publishStaffChat(payload);

        // Render on local server
        plugin.getChatManager().handleIncomingStaffChat(payload);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
