package dev.lyy.zelchatcross.commands;

import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command for clearing chat across all network servers (/gclearchat).
 */
public final class GlobalClearChatCommand implements CommandExecutor, TabCompleter {

    private final ZelChatCross plugin;

    public GlobalClearChatCommand(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelcross.admin.clearchat")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getModerationManager().clearGlobalChat(sender.getName());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
