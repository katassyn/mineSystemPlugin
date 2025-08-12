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
    private final boolean debug;
    private final NamespacedKey toolKey;

    public ToolListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("debug.toolListener", false);
        this.toolKey = new NamespacedKey(plugin, "custom_tool");
    }

    /**
     * Runs at HIGH priority so cancellations from this listener are respected
     * before the monitor phase, but after most game logic.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !tool.hasItemMeta()) {
            return;
        }

        boolean wasCancelled = event.isCancelled();
        Block block = event.getBlock();
        boolean insideSphere = plugin.getSphereManager().isInsideSphere(block.getLocation());
        boolean bypass = player.isOp() || player.hasPermission("minesystem.admin");

        if (debug) {
            plugin.getLogger().info(String.format(
                "[ToolListener] %s tried to break %s at %s (initially cancelled=%s)",
                player.getName(), block.getType(), block.getLocation(), wasCancelled));
            if (wasCancelled) {
                plugin.getLogger().info("[ToolListener] Event was already cancelled before processing");
            }
        }

        // restrict breaking inside spheres unless allowed
        if (insideSphere && !bypass && !canDestroy(tool, block)) {
            event.setCancelled(true);
            if (debug) {
                plugin.getLogger().info("[ToolListener] Cancelled: block not allowed inside sphere");
            }
        }

        // duplicate drops
        if (!event.isCancelled()) {
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

        boolean broken = CustomTool.damage(tool, plugin);
        if (broken) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItemInMainHand(tool);
        }
        if (debug) {
            plugin.getLogger().info("[ToolListener] Final state: " + (event.isCancelled() ? "cancelled" : "allowed"));
        }
        player.updateInventory();
    }

    private boolean canDestroy(ItemStack tool, Block block) {
        if (!tool.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = tool.getItemMeta();

        // Only allow tools created by this plugin
        if (!meta.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE)) {
            return false;
        }

        var canDestroy = meta.getCanDestroy();
        if (canDestroy == null || canDestroy.isEmpty()) {
            return false;
        }
        return canDestroy.contains(block.getType());
    }

}
