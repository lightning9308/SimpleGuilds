package me.lightning.simpleguilds;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class Guild {

    String name;
    UUID owner;
    int numMembers;
    Set<UUID> members = new HashSet<>();


    public void removeMember(UUID player) {
        this.members.remove(player);
        this.numMembers = members.size();
        GuildManager.saveGuilds();
    }

    public Boolean hasMember(UUID player) {
        return members.contains(player);
    }

    public void addMember(UUID player) {
        this.members.add(player);
        this.numMembers = members.size();
        GuildManager.saveGuilds();
    }

    public void setName(String name) {
        this.name = name;
        GuildManager.saveGuilds();
    }

    public String getName() {
        return name;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        GuildManager.saveGuilds();
    }

    public UUID getOwner() {
        return owner;
    }

    public int getNumMembers() {
        return numMembers;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void broadcast(String message) {
        members.stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(OfflinePlayer::isOnline)
                .forEach(p -> ((Player) p).sendMessage(message));
    }

    public void broadcast(String message,Player ignoredPlayer) {
        members.stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(OfflinePlayer::isOnline)
                .filter(p -> !p.equals(ignoredPlayer))
                .forEach(p -> ((Player) p).sendMessage(message));
    }

    public Guild(String guildName, UUID owner) {
        this.name = guildName;
        this.owner = owner;

        members.add(this.owner);
        this.numMembers = members.size();
    }
}
