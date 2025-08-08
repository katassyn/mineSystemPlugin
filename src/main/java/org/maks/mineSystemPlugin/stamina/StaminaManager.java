package org.maks.mineSystemPlugin.stamina;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.repository.QuestRepository;

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
    private final int baseMaxStamina;
    private final Map<UUID, PlayerStamina> staminaMap = new ConcurrentHashMap<>();
    private final Duration resetAfter;
    private final QuestRepository questRepository;
    private static final int STAMINA_PER_QUEST = 10;

    public StaminaManager(MineSystemPlugin plugin, int baseMaxStamina, Duration resetAfter, QuestRepository questRepository) {
        this.plugin = plugin;
        this.baseMaxStamina = baseMaxStamina;
        this.resetAfter = resetAfter;
        this.questRepository = questRepository;
        startResetTask();
    }

    private int calculateMaxStamina(UUID uuid) {
        try {
            return baseMaxStamina + questRepository.load(uuid).join()
                    .map(q -> q.progress() * STAMINA_PER_QUEST).orElse(0);
        } catch (Exception e) {
            e.printStackTrace();
            return baseMaxStamina;
        }
    }

    private PlayerStamina getData(UUID uuid) {
        PlayerStamina ps = staminaMap.computeIfAbsent(uuid, id -> new PlayerStamina(calculateMaxStamina(id)));
        int max = calculateMaxStamina(uuid);
        if (ps.getMaxStamina() != max) {
            ps.setMaxStamina(max);
            if (ps.getStamina() > max) {
                ps.setStamina(max);
            }
        }
        return ps;
    }

    private void startResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Instant now = Instant.now();
                staminaMap.forEach((uuid, ps) -> {
                    if (ps.getFirstUsage() != null && now.isAfter(ps.getFirstUsage().plus(resetAfter))) {
                        ps.setStamina(ps.getMaxStamina());
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
        return getData(uuid).getStamina();
    }

    public boolean hasStamina(UUID uuid, int amount) {
        return getStamina(uuid) >= amount;
    }

    public void deductStamina(UUID uuid, int amount) {
        PlayerStamina ps = getData(uuid);
        if (ps.getFirstUsage() == null) {
            ps.setFirstUsage(Instant.now());
        }
        ps.setStamina(Math.max(0, ps.getStamina() - amount));
    }
}
