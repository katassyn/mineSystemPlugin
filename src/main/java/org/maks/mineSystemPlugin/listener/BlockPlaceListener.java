package org.maks.mineSystemPlugin.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.maks.mineSystemPlugin.MineSystemPlugin;

/**
 * Marks ore blocks placed outside of mining spheres so they can be broken
 * normally without triggering custom mining restrictions.
 */
public class BlockPlaceListener implements Listener {

    private final MineSystemPlugin plugin;

    public BlockPlaceListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material type = block.getType();
        if (!type.name().endsWith("_ORE")) {
            return;
        }
        if (!plugin.getSphereManager().isInsideSphere(block.getLocation())) {
            plugin.markPlayerPlaced(block.getLocation());
        }
    }
}
