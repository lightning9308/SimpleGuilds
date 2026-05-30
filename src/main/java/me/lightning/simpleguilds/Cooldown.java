package me.lightning.simpleguilds;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cooldown {

    private final Map<UUID, Long> playerCooldown = new HashMap<>();

    private long currentTime() {
        return System.currentTimeMillis();
    }

    public void start(Player player, double seconds) {
        long duration = (long) (seconds * 1000);

        playerCooldown.put(player.getUniqueId(), currentTime() + duration);
    }

    // return true when not in map
    public boolean isOver(Player player) {
        if (!playerCooldown.containsKey(player.getUniqueId())) return true;

        if (currentTime() >= playerCooldown.get(player.getUniqueId())) {
            playerCooldown.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    // return false when not in map
    public boolean isFinished(Player player) {
        if (!playerCooldown.containsKey(player.getUniqueId())) return false;

        if (currentTime() >= playerCooldown.get(player.getUniqueId())) {
            playerCooldown.remove(player.getUniqueId());
            return true;
        }
        return false;
    }
}
