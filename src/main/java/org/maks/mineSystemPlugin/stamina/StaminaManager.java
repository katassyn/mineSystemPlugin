package org.maks.mineSystemPlugin.stamina;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.repository.QuestRepository;
import org.maks.mineSystemPlugin.repository.PlayerRepository;
import org.maks.mineSystemPlugin.model.PlayerData;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Handles stamina usage and timed resets for players.
 */
public class StaminaManager {
    private final MineSystemPlugin plugin;
    private final int baseMaxStamina;
    private final Map<UUID, PlayerStamina> staminaMap = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> saveQueue = new ConcurrentHashMap<>();
    private final Duration resetAfter;
    private final QuestRepository questRepository;
    private final PlayerRepository playerRepository;
    private static final int STAMINA_PER_QUEST = 10;

    public StaminaManager(MineSystemPlugin plugin, int baseMaxStamina, Duration resetAfter,
                          QuestRepository questRepository, PlayerRepository playerRepository) {
        this.plugin = plugin;
        this.baseMaxStamina = baseMaxStamina;
        this.resetAfter = resetAfter;
        this.questRepository = questRepository;
        this.playerRepository = playerRepository;
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
        PlayerStamina ps = staminaMap.computeIfAbsent(uuid, id -> {
            int maxStamina = calculateMaxStamina(id);
            PlayerStamina s = new PlayerStamina(maxStamina);

            // Ważne: najpierw próbujemy załadować z bazy
            boolean dataLoaded = false;
            try {
                var futureData = playerRepository.load(id);
                var optionalData = futureData.join();

                if (optionalData.isPresent()) {
                    PlayerData data = optionalData.get();
                    plugin.getLogger().info("Loading stamina for player " + id + ": " + data.stamina() + "/" + maxStamina);

                    // Ustawiamy wartość z bazy danych
                    s.setStamina(data.stamina());

                    // Ustawiamy timestamp resetu jeśli istnieje
                    if (data.resetTimestamp() > 0) {
                        s.setFirstUsage(Instant.ofEpochMilli(data.resetTimestamp()));

                        // Sprawdzamy czy minął czas resetu
                        Instant now = Instant.now();
                        Instant resetTime = s.getFirstUsage().plus(resetAfter);
                        if (now.isAfter(resetTime)) {
                            plugin.getLogger().info("Reset time passed for " + id + ", resetting stamina to max");
                            s.setStamina(maxStamina);
                            s.setFirstUsage(null);
                            // Zapisz zaktualizowane dane
                            queueSave(id, s);
                        }
                    }
                    dataLoaded = true;
                } else {
                    plugin.getLogger().info("No data found for player " + id + " in database");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load stamina for player " + id + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Tylko jeśli NIE udało się załadować danych, traktuj jako nowego gracza
            if (!dataLoaded) {
                plugin.getLogger().info("Treating " + id + " as new player, setting full stamina: " + maxStamina);
                s.setStamina(maxStamina);
                // Zapisz nowego gracza do bazy
                queueSave(id, s);
            }

            return s;
        });

        // Aktualizuj max stamina jeśli się zmieniła (np. przez questy)
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
                    if (ps.getFirstUsage() != null) {
                        Instant resetTime = ps.getFirstUsage().plus(resetAfter);
                        if (now.isAfter(resetTime)) {
                            plugin.getLogger().info("Resetting stamina for " + uuid + " (timer expired)");
                            ps.setStamina(ps.getMaxStamina());
                            ps.setFirstUsage(null);
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                p.sendMessage("Your stamina has been refreshed.");
                            }
                            queueSave(uuid, ps);
                        }
                    }
                });
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // check every minute
    }

    public int getStamina(UUID uuid) {
        return getData(uuid).getStamina();
    }

    public int getMaxStamina(UUID uuid) {
        return getData(uuid).getMaxStamina();
    }

    public Duration getTimeUntilReset(UUID uuid) {
        PlayerStamina ps = getData(uuid);
        if (ps.getFirstUsage() == null) {
            return Duration.ZERO;
        }
        Instant resetTime = ps.getFirstUsage().plus(resetAfter);
        Instant now = Instant.now();
        if (now.isAfter(resetTime)) {
            return Duration.ZERO;
        }
        return Duration.between(now, resetTime);
    }

    public boolean hasStamina(UUID uuid, int amount) {
        // During Miner Day event, stamina is unlimited
        if (isMinerDayActive()) {
            return true;
        }
        return getStamina(uuid) >= amount;
    }

    /**
     * Check if miner_day event is active using EventPlugin API.
     */
    private boolean isMinerDayActive() {
        try {
            Class<?> apiClass = Class.forName("org.maks.eventPlugin.api.EventPluginAPI");
            Method isActiveMethod = apiClass.getMethod("isEventActive", String.class);
            return (boolean) isActiveMethod.invoke(null, "miner_day");
        } catch (Exception e) {
            return false;
        }
    }

    public void deductStamina(UUID uuid, int amount) {
        // During Miner Day event, don't deduct stamina
        if (isMinerDayActive()) {
            return;
        }

        PlayerStamina ps = getData(uuid);
        if (ps.getFirstUsage() == null && amount > 0) {
            ps.setFirstUsage(Instant.now());
            plugin.getLogger().info("Starting stamina timer for " + uuid);
        }
        int newStamina = Math.max(0, ps.getStamina() - amount);
        ps.setStamina(newStamina);

        plugin.getLogger().info("Deducting " + amount + " stamina from " + uuid + ", new value: " + newStamina);
        queueSave(uuid, ps);
    }

    /**
     * Restores a player's stamina to its maximum and clears any active reset timer.
     */
    public void refillStamina(UUID uuid) {
        PlayerStamina ps = getData(uuid);
        ps.setStamina(ps.getMaxStamina());
        ps.setFirstUsage(null);
        plugin.getLogger().info("Refilling stamina for " + uuid + " to max: " + ps.getMaxStamina());
        queueSave(uuid, ps);
    }

    public void saveAll() {
        plugin.getLogger().info("Saving all stamina data...");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        staminaMap.forEach((uuid, ps) -> {
            long reset = ps.getFirstUsage() == null ? 0L : ps.getFirstUsage().toEpochMilli();
            plugin.getLogger().info("Saving stamina for " + uuid + ": " + ps.getStamina() + ", reset: " + reset);
            futures.add(queueSave(uuid, ps));
        });
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * Force reload stamina data from database for a specific player.
     * Useful for debugging or after manual database changes.
     */
    public void reloadPlayerData(UUID uuid) {
        plugin.getLogger().info("Reloading stamina data for " + uuid);
        staminaMap.remove(uuid);
        getData(uuid); // This will force reload from database
    }

    private CompletableFuture<Void> queueSave(UUID uuid, PlayerStamina ps) {
        long reset = ps.getFirstUsage() == null ? 0L : ps.getFirstUsage().toEpochMilli();
        PlayerData snapshot = new PlayerData(uuid, ps.getStamina(), reset);

        CompletableFuture<Void> next = saveQueue.compute(uuid, (id, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null)
                    : previous;
            CompletableFuture<Void> chained = base.handle((ignored, error) -> {
                if (error != null) {
                    plugin.getLogger().log(Level.WARNING,
                            "Previous stamina save for " + id + " failed", error);
                }
                return null;
            }).thenCompose(v -> playerRepository.save(snapshot));

            chained.whenComplete((ignored, error) -> {
                if (error != null) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to save stamina data for " + id, error);
                }
                saveQueue.compute(id, (key, current) -> current == chained ? null : current);
            });
            return chained;
        });

        return next == null ? CompletableFuture.completedFuture(null) : next;
    }
}
