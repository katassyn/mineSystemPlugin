package org.maks.mineSystemPlugin.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.player.Player;
import org.maks.mineSystemPlugin.MineSystemPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Handles custom mining logic. Each ore requires a configured number of hits
 * before it breaks. When the threshold is reached a MythicMobs item with the
 * same id as the ore is given to the player and the block is removed.
 */
public class BlockBreakListener implements Listener {

    private static final List<String> ORE_REWARDS = Arrays.asList("ore_I", "ore_II", "ore_III");

    private final MineSystemPlugin plugin;
    private final Random random = new Random();

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
        if (random.nextDouble() < 0.10) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    String.format("mm items give %s %s 1", player.getName(), oreId));
        }

        // Remove the block and prevent default drops
        event.setDropItems(false);
        block.setType(Material.AIR);

        int total = plugin.incrementOreCount();
        if (total % 20 == 0) {
            String rewardName = ORE_REWARDS.get(random.nextInt(ORE_REWARDS.size()));
            ItemStack reward = MythicBukkit.inst().getItemManager().getItemStack(rewardName);
            if (reward != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), reward);
            }
        }
    }
}
