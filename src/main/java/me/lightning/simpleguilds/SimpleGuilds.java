package me.lightning.simpleguilds;

import me.lightning.simpleguilds.commands.GuildChatCommand;
import me.lightning.simpleguilds.commands.GuildCommand;
import me.lightning.simpleguilds.events.PlayerJoinListener;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

public final class SimpleGuilds extends JavaPlugin {
    private static SimpleGuilds plugin;
    private static BukkitAudiences adventure;

    @NonNull
    public static BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }
    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        plugin = this;

        GuildManager.loadGuilds();

        getCommand("guild").setExecutor(new GuildCommand());
        getCommand("guildchat").setExecutor(new GuildChatCommand());

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(),this);

    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        GuildManager.saveGuilds();
    }

    public static SimpleGuilds getPlugin() {return plugin;}
}
