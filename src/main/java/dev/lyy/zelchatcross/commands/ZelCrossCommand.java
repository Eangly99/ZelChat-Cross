package dev.lyy.zelchatcross.commands;

import dev.lyy.zelchatcross.ZelChatCross;
import net.kyori.adventure.text.Component;
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
import java.util.Map;

/**
 * Main administrative command for ZelChat-Cross (/zelcross).
 */
public final class ZelCrossCommand implements CommandExecutor, TabCompleter {

    private final ZelChatCross plugin;

    public ZelCrossCommand(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("status")) {
            sendInfo(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("zelcross.admin.reload")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
                return true;
            }
            case "debug" -> {
                if (!sender.hasPermission("zelcross.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                boolean current = plugin.getConfigManager().isDebug();
                plugin.getConfigManager().setDebug(!current);
                sender.sendMessage(plugin.getConfigManager().parse("<green>ZelCross verbose debug mode is now <bold>"
                        + (!current ? "ENABLED" : "DISABLED") + "</bold>.</green>"));
                return true;
            }
            case "servers", "list" -> {
                if (!sender.hasPermission("zelcross.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                sendServerList(sender);
                return true;
            }
            case "viewinv", "viewinventory" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                if (!player.hasPermission("zelcross.showcase.view")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + label + " viewinv <snapshotId></red>"));
                    return true;
                }
                plugin.getShowcaseManager().openSnapshotGui(player, args[1]);
                return true;
            }
            case "viewec", "viewenderchest" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                if (!player.hasPermission("zelcross.showcase.view")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + label + " viewec <snapshotId></red>"));
                    return true;
                }
                plugin.getShowcaseManager().openSnapshotGui(player, args[1]);
                return true;
            }
            case "viewitem" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
                    return true;
                }
                if (!player.hasPermission("zelcross.showcase.view")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + label + " viewitem <snapshotId></red>"));
                    return true;
                }
                plugin.getShowcaseManager().openSnapshotGui(player, args[1]);
                return true;
            }
            default -> {
                sender.sendMessage(plugin.getConfigManager().parse("<red>Unknown subcommand. Use /" + label + " [reload|debug|info|servers|viewinv|viewec]</red>"));
                return true;
            }
        }
    }

    private void sendInfo(CommandSender sender) {
        List<String> lines = plugin.getConfigManager().getMessageList("info-message");
        int networkPlayers = plugin.getPresenceManager().getNetworkOnlineCount();
        int serverCount = plugin.getPresenceManager().getServerPlayerCounts().size();
        String version = plugin.getPluginMeta().getVersion();
        String serverId = plugin.getConfigManager().getServerId();

        for (String line : lines) {
            String formatted = line
                    .replace("{version}", version)
                    .replace("{server_id}", serverId)
                    .replace("{network_players}", String.valueOf(networkPlayers))
                    .replace("{server_count}", String.valueOf(serverCount));
            sender.sendMessage(plugin.getConfigManager().parse(formatted));
        }
    }

    private void sendServerList(CommandSender sender) {
        int totalPlayers = plugin.getPresenceManager().getNetworkOnlineCount();
        Component header = plugin.getConfigManager().getMessage("server-list-header",
                Map.of("count", String.valueOf(totalPlayers)));
        sender.sendMessage(header);

        Map<String, Integer> serverCounts = plugin.getPresenceManager().getServerPlayerCounts();
        for (Map.Entry<String, Integer> entry : serverCounts.entrySet()) {
            String serverId = entry.getKey();
            int count = entry.getValue();
            Component entryMsg = plugin.getConfigManager().getMessage("server-list-entry",
                    Map.of("server", serverId, "players", String.valueOf(count)));
            sender.sendMessage(entryMsg);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> subs = List.of("info", "reload", "debug", "servers", "viewinv", "viewec", "viewitem");
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return List.of();
    }
}
