package org.maks.mineSystemPlugin.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.maks.mineSystemPlugin.MineSystemPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spawns MythicMobs at a location using configured identifiers.
 */
public class MobSpawner {
    private final MineSystemPlugin plugin;
    private final List<MobEntry> entries = new ArrayList<>();
    private final Random random = new Random();

    public MobSpawner(MineSystemPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        entries.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mobSpawns");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String id = section.getString(key + ".id");
            int amount = section.getInt(key + ".amount", 1);
            int weight = section.getInt(key + ".weight", 1);
            if (id != null && amount > 0 && weight > 0) {
                entries.add(new MobEntry(id, amount, weight));
            }
        }
    }

    public void spawn(Location loc) {
        if (entries.isEmpty() || loc == null) {
            return;
        }
        MobEntry entry = getRandomEntry();
        String cmd = String.format("mm m spawn %s %d %d %d %d", entry.id, entry.amount,
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private MobEntry getRandomEntry() {
        int totalWeight = entries.stream().mapToInt(e -> e.weight).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (MobEntry entry : entries) {
            cumulative += entry.weight;
            if (roll < cumulative) {
                return entry;
            }
        }
        return entries.get(0);
    }

    private record MobEntry(String id, int amount, int weight) {}
}
