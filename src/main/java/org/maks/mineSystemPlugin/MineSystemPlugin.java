package org.maks.mineSystemPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import org.maks.mineSystemPlugin.listener.BlockBreakListener;

import java.util.HashMap;
import java.util.Map;

public final class MineSystemPlugin extends JavaPlugin {

    /** Mapping of ore id to required hits loaded from configuration. */
    private final Map<String, Integer> oreHits = new HashMap<>();

    /** Tracks remaining hits for blocks currently being mined. */
    private final Map<String, Integer> blockHits = new HashMap<>();

    private NamespacedKey oreKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        oreKey = new NamespacedKey(this, "oreId");
        loadOreHits();

        // Register events
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
    }

    @Override
    public void onDisable() {
        blockHits.clear();
        oreHits.clear();
    }

    private void loadOreHits() {
        oreHits.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("oreHits");
        if (section == null) {
            getLogger().warning("No oreHits section found in config");
            return;
        }

        for (String key : section.getKeys(false)) {
            oreHits.put(key, section.getInt(key));
        }
    }

    /**
     * Resolves the ore id for a block. If the block has a persistent data entry
     * named "oreId" that value is used; otherwise the material name of the block
     * is returned.
     */
    public String resolveOreId(Block block) {
        if (block.getState() instanceof TileState tile) {
            PersistentDataContainer container = tile.getPersistentDataContainer();
            String id = container.get(oreKey, PersistentDataType.STRING);
            if (id != null) {
                return id;
            }
        }
        return block.getType().name();
    }

    /**
     * Decrements the hit counter for a block at the given location and returns
     * the remaining hits required before the block breaks.
     */
    public int decrementBlockHits(org.bukkit.Location location, String oreId) {
        int maxHits = oreHits.getOrDefault(oreId, 1);
        String key = key(location);
        int remaining = blockHits.getOrDefault(key, maxHits);
        remaining--;
        if (remaining <= 0) {
            blockHits.remove(key);
        } else {
            blockHits.put(key, remaining);
        }
        return remaining;
    }

    private String key(org.bukkit.Location location) {
        return location.getWorld().getName() + ':' + location.getBlockX() + ':' +
                location.getBlockY() + ':' + location.getBlockZ();
    }
}
