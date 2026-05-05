package me.lightning.simpleguilds;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestManager {
    private static final Map<UUID,Request> requests = new HashMap<>();

    public static void addRequest(Request request) {

        requests.put(request.getTarget(), request);

        Bukkit.getScheduler().runTaskLater(SimpleGuilds.getPlugin(),() -> {
            if (requests.containsKey(request.getTarget()) && requests.get(request.getTarget()) == request) {
                requests.remove(request.getTarget());

                //send the requester that the request has expired
                Player sender = Bukkit.getPlayer(request.getSender());
                if (sender != null) {
                    sender.sendMessage("§6§lSimpleGuilds §7§l> §cYour "+ request.getType().toString().toLowerCase()+" request has expired.");
                }
            }
        },20L * 60);
    }

    public static Request getRequest(UUID player) {
        return requests.get(player);
    }

    public static void removeRequest(UUID player) {
        requests.remove(player);
    }

}
