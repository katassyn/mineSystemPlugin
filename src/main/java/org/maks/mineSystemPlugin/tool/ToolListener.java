package org.maks.mineSystemPlugin.tool;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Listener handling custom tool behaviour such as durability and duplicate drops.
 */
public class ToolListener implements Listener {

    private final Plugin plugin;

    public ToolListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR) {
            return;
        }

        if (!tool.hasItemMeta()) return;

        // check canDestroy list
        if (!canDestroy(tool, event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        // duplicate drops
        int dupLevel = CustomTool.getDuplicateLevel(tool, plugin);
        if (dupLevel > 0) {
            double chance = switch (dupLevel) {
                case 1 -> 0.03;
                case 2 -> 0.04;
                case 3 -> 0.05;
                default -> 0.0;
            };
            if (Math.random() < chance) {
                Collection<ItemStack> drops = event.getBlock().getDrops(tool, player);
                for (ItemStack drop : drops) {
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                }
            }
        }

        // durability handling
        boolean broken = CustomTool.damage(tool, plugin);
        if (broken) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItemInMainHand(tool);
        }
        player.updateInventory();
    }

    private boolean canDestroy(ItemStack tool, Block block) {
        if (!tool.hasItemMeta()) return true;
        var meta = tool.getItemMeta();
        var canDestroy = meta.getCanDestroy();
        if (canDestroy == null || canDestroy.isEmpty()) return true;
        return canDestroy.contains(block.getType());
    }
}
