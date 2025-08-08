package org.maks.mineSystemPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.events.OreMinedEvent;
import org.maks.mineSystemPlugin.item.CustomItems;
import org.maks.mineSystemPlugin.tool.CustomTool;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Handles custom mining logic. Each ore requires a configured number of hits
 * before it breaks. When the threshold is reached an item matching the
 * items.yml definition is dropped and the block is removed.
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
        Material oreType = block.getType();

        String oreId = plugin.resolveOreId(block);
        int remaining = plugin.decrementBlockHits(block.getLocation(), oreId);

        plugin.getSphereManager().updateHologram(block.getLocation(), remaining);

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
        ItemStack drop = CustomItems.get(oreId);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            if (duplicate) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }

        block.setType(Material.AIR);

        int amount = drop == null ? 0 : (duplicate ? drop.getAmount() * 2 : drop.getAmount());
        int pickaxeLevel = CustomTool.getToolLevel(tool);
        Bukkit.getPluginManager().callEvent(new OreMinedEvent(player, oreType, amount, pickaxeLevel));

        int total = plugin.incrementOreCount();
        if (total % 20 == 0) {
            int bonus = random.nextInt(3) + 1;
            for (int i = 0; i < bonus; i++) {
                String rewardId = BONUS_ITEMS.get(random.nextInt(BONUS_ITEMS.size()));
                ItemStack reward = CustomItems.get(rewardId);
                if (reward != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), reward.clone());
                }
            }
        }

        boolean broken = CustomTool.damage(tool, plugin);
        if (broken) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItemInMainHand(tool);
        }
    }
}
