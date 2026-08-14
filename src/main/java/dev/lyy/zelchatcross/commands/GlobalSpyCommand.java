package dev.lyy.zelchatcross.commands;

import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command for toggling SocialSpy across network private messages (/gspy, /socialspy).
 */
public final class GlobalSpyCommand implements CommandExecutor, TabCompleter {

    private final ZelChatCross plugin;

    public GlobalSpyCommand(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("zelcross.admin.spy")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        boolean enabled = plugin.getPrivateMessageManager().toggleSpy(player);
        if (enabled) {
            player.sendMessage(plugin.getConfigManager().getMessage("spy-enabled"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("spy-disabled"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
