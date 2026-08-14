package dev.lyy.zelchatcross.commands;

import dev.lyy.zelchatcross.ZelChatCross;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command for muting or unmuting chat across all network instances (/gchatmute).
 */
public final class GlobalChatMuteCommand implements CommandExecutor, TabCompleter {

    private final ZelChatCross plugin;

    public GlobalChatMuteCommand(ZelChatCross plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zelcross.admin.chatmute")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        boolean newMuteStatus;
        if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            if (arg.equals("on") || arg.equals("true") || arg.equals("enable") || arg.equals("mute")) {
                newMuteStatus = true;
            } else if (arg.equals("off") || arg.equals("false") || arg.equals("disable") || arg.equals("unmute")) {
                newMuteStatus = false;
            } else {
                sender.sendMessage(plugin.getConfigManager().parse("<red>Usage: /" + label + " [on|off]</red>"));
                return true;
            }
        } else {
            // Toggle
            newMuteStatus = !plugin.getModerationManager().isChatMuted();
        }

        plugin.getModerationManager().setGlobalChatMuted(sender.getName(), newMuteStatus);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("on", "off", "toggle");
            List<String> matches = new ArrayList<>();
            for (String opt : options) {
                if (opt.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    matches.add(opt);
                }
            }
            return matches;
        }
        return List.of();
    }
}
