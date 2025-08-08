package org.maks.mineSystemPlugin.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
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
import java.util.List;
import java.util.Random;

/**
 * Handles custom mining logic. Each ore requires a configured number of hits
 * before it breaks. When the threshold is reached a Mythic item defined in
 * items.yml is dropped and the block is removed.
 */
public class BlockBreakListener implements Listener {

    private static final List<String> BONUS_ITEMS =
            Arrays.asList("ore_I", "ore_II", "ore_III");

    private final MineSystemPlugin plugin;
    private final Random random = new Random();

    public BlockBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.isCustomOre(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        String oreId = plugin.resolveOreId(block);
        int remaining = plugin.decrementBlockHits(block.getLocation(), oreId);

        event.setCancelled(true);

        if (remaining > 0) {
            return;
        }

        event.setDropItems(false);

        int dupLevel = CustomTool.getDuplicateLevel(tool, plugin);
        double chance = switch (dupLevel) {
            case 1 -> 0.03;
            case 2 -> 0.04;
            case 3 -> 0.05;
            default -> 0.0;
        };

        boolean duplicate = Math.random() < chance;
        ItemStack drop = MythicBukkit.inst().getItemManager().getItemStack(oreId);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            if (duplicate) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }

        block.setType(Material.AIR);

        int total = plugin.incrementOreCount();
        if (total % 20 == 0) {
            String rewardId = BONUS_ITEMS.get(random.nextInt(BONUS_ITEMS.size()));
            ItemStack reward = MythicBukkit.inst().getItemManager().getItemStack(rewardId);
            if (reward != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), reward);
            }
        }
    }
}
