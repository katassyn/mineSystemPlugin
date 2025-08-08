package org.maks.mineSystemPlugin.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.player.Player;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.tool.CustomTool;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Handles custom mining logic. Each ore requires a configured number of hits
 * before it breaks. When the threshold is reached the block's normal drops are
 * spawned and the block is removed.
 */
public class BlockBreakListener implements Listener {

    private static final List<Material> ORE_REWARDS =
            Arrays.asList(Material.COAL, Material.IRON_INGOT, Material.DIAMOND);

    private final MineSystemPlugin plugin;
    private final Random random = new Random();

    public BlockBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        String oreId = plugin.resolveOreId(block);
        int remaining = plugin.decrementBlockHits(block.getLocation(), oreId);

        // Always cancel default behaviour to control drops and block removal
        event.setCancelled(true);

        if (remaining > 0) {
            // Block still has hits remaining; do not break it yet
            return;
        }

        Collection<ItemStack> drops = block.getDrops(tool, player);
        event.setDropItems(false);

        int dupLevel = CustomTool.getDuplicateLevel(tool, plugin);
        double chance = switch (dupLevel) {
            case 1 -> 0.03;
            case 2 -> 0.04;
            case 3 -> 0.05;
            default -> 0.0;
        };

        boolean duplicate = Math.random() < chance;
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            if (duplicate) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }

        // Remove the block
        block.setType(Material.AIR);

        int total = plugin.incrementOreCount();
        if (total % 20 == 0) {
            Material rewardMat = ORE_REWARDS.get(random.nextInt(ORE_REWARDS.size()));
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(rewardMat));
        }
    }
}
