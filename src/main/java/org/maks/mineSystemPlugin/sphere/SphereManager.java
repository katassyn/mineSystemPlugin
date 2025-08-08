package org.maks.mineSystemPlugin.sphere;

import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.mob.MobSpawner;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks active spheres and enforces a global limit.
 */
public class SphereManager {
    private final MineSystemPlugin plugin;
    private final int limit;
    private final Set<UUID> active = new HashSet<>();
    private final MobSpawner mobSpawner;

    /** Placeholder block replaced by actual ores when generating spheres. */
    public static final Material ORE_PLACEHOLDER = Material.WHITE_WOOL;

    public SphereManager(MineSystemPlugin plugin, int limit, MobSpawner mobSpawner) {
        this.plugin = plugin;
        this.limit = limit;
        this.mobSpawner = mobSpawner;
    }

    public boolean canCreateSphere(Player player) {
        if (active.size() >= limit) {
            player.sendMessage("Too many active spheres. Please wait.");
            return false;
        }
        return true;
    }

    public void registerSphere(Player player) {
        active.add(player.getUniqueId());
        Location center = player.getLocation();
        mobSpawner.spawn(center);
    }

    public void removeSphere(Player player) {
        active.remove(player.getUniqueId());
    }
}
