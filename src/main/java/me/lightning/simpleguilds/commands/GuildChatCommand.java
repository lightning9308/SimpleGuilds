package me.lightning.simpleguilds.commands;

import me.lightning.simpleguilds.Guild;
import me.lightning.simpleguilds.GuildManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuildChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(commandSender instanceof Player sender)) {
            commandSender.sendMessage("Only players can execute this command");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§6§lSimpleGuilds §7§l> §4Usage: /gc <message>");
            return true;
        }
        Guild guild = GuildManager.getPlayerGuild(sender);
        if (guild == null) {
            sender.sendMessage("§6§lSimpleGuilds §7§l> §4You’re not in a guild. Create one with /guild create <name> or join with /guild join <guild>.");
            return true;
        }
        String message = String.join(" ",args);
        guild.broadcast("§6§lGuild §7§l> §2" + sender.getName() + "§r: " + message);
        return true;
    }
}
