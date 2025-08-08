package org.maks.mineSystemPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.player.Player;
import org.maks.mineSystemPlugin.MineSystemPlugin;

/**
 * Handles custom mining logic. Each ore requires a configured number of hits
 * before it breaks. When the threshold is reached a MythicMobs item with the
 * same id as the ore is given to the player and the block is removed.
 */
public class BlockBreakListener implements Listener {

    private final MineSystemPlugin plugin;

    public BlockBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        String oreId = plugin.resolveOreId(block);
        int remaining = plugin.decrementBlockHits(block.getLocation(), oreId);

        // Always cancel default behaviour to control drops and block removal
        event.setCancelled(true);

        if (remaining > 0) {
            // Block still has hits remaining; do not break it yet
            return;
        }

        // Give MythicMobs item via command dispatch to avoid compile dependency
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                String.format("mm items give %s %s 1", player.getName(), oreId));

        // Remove the block and prevent default drops
        event.setDropItems(false);
        block.setType(Material.AIR);
    }
}
