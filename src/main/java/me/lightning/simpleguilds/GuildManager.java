package me.lightning.simpleguilds;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class GuildManager {

    public enum GuildCreateResult {
        SUCCESS,
        NAME_TOO_SHORT,
        NAME_TOO_LONG,
        NAME_TAKEN
    }

    private static Set<Guild> guildSet = new HashSet<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type type = new TypeToken<Set<Guild>>() {}.getType();
    private static final File file = new File(SimpleGuilds.getPlugin().getDataFolder(), "guilds.json");

    public static GuildCreateResult createGuild(String guildName, UUID owner) {
        //Check length
        if (guildName.length() < 3) return GuildCreateResult.NAME_TOO_SHORT;
        if (guildName.length() > 30) return GuildCreateResult.NAME_TOO_LONG;

        //Check Name
        for (Guild guild : guildSet) {
            if (guild.getName().equalsIgnoreCase(guildName)) return GuildCreateResult.NAME_TAKEN;
        }
        //Create guild
        Guild guild = new Guild(guildName, owner);
        guildSet.add(guild);
        saveGuilds();
        return GuildCreateResult.SUCCESS;

    }

    public static void deleteGuild(Guild guild) {
        guildSet.remove(guild);
        saveGuilds();
    }

    public static Guild getPlayerGuild(Player player) {
        for (Guild guild : guildSet) {
            if (guild.hasMember(player.getUniqueId())) return guild;
        }
        return null;
    }

    public static Guild getGuild(String guildName) {
        for (Guild guild : guildSet) {
            if (guild.getName().equals(guildName)) return guild;
        }
        return null;
    }

    public static Set<Guild> getAllGuilds() {
        return guildSet;
    }

    public static void saveGuilds() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            File tempFile = new File(file.getParentFile(), "guilds.json.tmp");

            try (Writer writer = new FileWriter(tempFile)) {
                gson.toJson(getAllGuilds(), type, writer);
            }
            java.nio.file.Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadGuilds() {
        if (!(file.exists())) {
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Set<Guild> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                guildSet.clear();
                guildSet.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

