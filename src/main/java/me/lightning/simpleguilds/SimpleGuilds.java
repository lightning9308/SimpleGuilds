package me.lightning.simpleguilds;

import me.lightning.simpleguilds.commands.GuildChatCommand;
import me.lightning.simpleguilds.commands.GuildCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleGuilds extends JavaPlugin {
    private static SimpleGuilds plugin;

    @Override
    public void onEnable() {
        plugin = this;
        GuildManager.loadGuilds();
        getCommand("guild").setExecutor(new GuildCommand());
        getCommand("guildchat").setExecutor(new GuildChatCommand());

    }

    @Override
    public void onDisable() {

    }

    public static SimpleGuilds getPlugin() {return plugin;}
}
