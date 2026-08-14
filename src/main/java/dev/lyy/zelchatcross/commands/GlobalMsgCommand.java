package dev.lyy.zelchatcross.commands;

import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command for cross-server private messaging with network-wide tab-completion (/msg, /tell, /w).
 */
public final class GlobalMsgCommand implements CommandExecutor, TabCompleter {

    private final ZelChatCross plugin;

    public GlobalMsgCommand(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("zelcross.msg")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + label + " <player> <message></red>"));
            return true;
        }

        String targetName = args[0];
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString().trim();

        if (message.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-send-empty"));
            return true;
        }

        plugin.getPrivateMessageManager().sendPrivateMessage(player, targetName, message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && plugin.getConfigManager().isPresenceTabComplete()) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String name : plugin.getPresenceManager().getOnlinePlayerNames()) {
                if (name.equalsIgnoreCase(sender.getName())) continue;
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    matches.add(name);
                }
            }
            return matches;
        }
        return List.of();
    }
}
