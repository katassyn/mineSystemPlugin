package org.maks.mineSystemPlugin.tool;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.maks.mineSystemPlugin.MineSystemPlugin;

/**
 * Listener handling custom tool behaviour such as durability and duplicate drops.
 */
public class ToolListener implements Listener {

    private final MineSystemPlugin plugin;

    public ToolListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !tool.hasItemMeta()) {
            return;
        }

        if (!event.isCancelled()) {
            Block block = event.getBlock();
            boolean insideSphere = plugin.getSphereManager().isInsideSphere(block.getLocation());
            boolean bypass = player.isOp() || player.hasPermission("minesystem.admin");

            // restrict breaking inside spheres unless allowed
            if (insideSphere && !bypass && !canDestroy(tool, block)) {
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
        }

        // durability handling
        CustomTool.ensureDurability(tool, plugin);

        int before = readDurability(tool);
        plugin.getLogger().info(String.format("Tool %s durability before: %d (unbreakable=%s)",
                tool.getType(), before, tool.getItemMeta().isUnbreakable()));
        boolean broken = CustomTool.damage(tool, plugin);
        int after = readDurability(tool);
        plugin.getLogger().info(String.format("Tool %s durability after: %d (broken=%s)",
                tool.getType(), after, broken));
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

    private int readDurability(ItemStack tool) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return -1;
        Integer cur = meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "durability"), PersistentDataType.INTEGER);
        return cur == null ? -1 : cur;
    }
}
