package org.maks.mineSystemPlugin.stamina;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.maks.mineSystemPlugin.MineSystemPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles stamina usage and timed resets for players.
 */
public class StaminaManager {
    private final MineSystemPlugin plugin;
    private final int maxStamina;
    private final Map<UUID, PlayerStamina> staminaMap = new ConcurrentHashMap<>();
    private final Duration resetAfter;

    public StaminaManager(MineSystemPlugin plugin, int maxStamina, Duration resetAfter) {
        this.plugin = plugin;
        this.maxStamina = maxStamina;
        this.resetAfter = resetAfter;
        startResetTask();
    }

    private void startResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Instant now = Instant.now();
                staminaMap.forEach((uuid, ps) -> {
                    if (ps.getFirstUsage() != null && now.isAfter(ps.getFirstUsage().plus(resetAfter))) {
                        ps.setStamina(maxStamina);
                        ps.setFirstUsage(null);
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendMessage("Your stamina has been refreshed.");
                        }
                    }
                });
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // check every minute
    }

    public int getStamina(UUID uuid) {
        return staminaMap.computeIfAbsent(uuid, id -> new PlayerStamina(maxStamina)).getStamina();
    }

    public boolean hasStamina(UUID uuid, int amount) {
        return getStamina(uuid) >= amount;
    }

    public void deductStamina(UUID uuid, int amount) {
        PlayerStamina ps = staminaMap.computeIfAbsent(uuid, id -> new PlayerStamina(maxStamina));
        if (ps.getFirstUsage() == null) {
            ps.setFirstUsage(Instant.now());
        }
        ps.setStamina(Math.max(0, ps.getStamina() - amount));
    }
}
