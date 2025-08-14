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
import org.bukkit.persistence.PersistentDataContainer;
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

        // initialise durability and marker metadata before any checks
        CustomTool.ensureDurability(tool, plugin);

        boolean wasCancelled = event.isCancelled();
        Block block = event.getBlock();
        boolean insideSphere = plugin.getSphereManager().isInsideSphere(block.getLocation());
        boolean bypass = player.isOp() || player.hasPermission("minesystem.admin");
        boolean pluginTool = isPluginTool(tool);
        boolean allowed = pluginTool && canDestroy(tool, block);

        if (debug) {
            plugin.getLogger().info(String.format(
                "[ToolListener] %s tried to break %s at %s (initially cancelled=%s)",
                player.getName(), block.getType(), block.getLocation(), wasCancelled));
            if (wasCancelled) {
                plugin.getLogger().info("[ToolListener] Event was already cancelled before processing");
            }
            plugin.getLogger().info("[ToolListener] pluginTool=" + pluginTool + ", allowed=" + allowed + ", tool=" + tool.getType());

            ItemMeta meta = tool.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                plugin.getLogger().info(
                    "[ToolListener] hasCustomToolKey=" + pdc.has(toolKey, PersistentDataType.BYTE));
                plugin.getLogger().info("[ToolListener] canDestroy=" + meta.getCanDestroy());
            }
        }

        // restrict breaking inside spheres unless allowed
        if (insideSphere && !bypass && !allowed) {
            event.setCancelled(true);
            if (debug) {
                plugin.getLogger().info("[ToolListener] Cancelled: block not allowed inside sphere");
            }
            return;
        }

        if (!pluginTool) {
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

        if (debug) {
            int[] before = CustomTool.getDurability(tool, plugin);
            if (before != null) {
                plugin.getLogger().info(
                    "[ToolListener] Durability before hit: " + before[0] + "/" + before[1]);
            } else {
                plugin.getLogger().info("[ToolListener] Durability data missing before hit");
            }
        }


        boolean broken = CustomTool.damage(tool, plugin);

        if (debug) {
            int[] after = CustomTool.getDurability(tool, plugin);
            if (after != null) {
                plugin.getLogger().info(
                    "[ToolListener] Durability after hit: " + after[0] + "/" + after[1]);
            } else {
                plugin.getLogger().info("[ToolListener] Durability data missing after hit");
            }
            plugin.getLogger().info("[ToolListener] Broken after hit: " + broken);
        }
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

    private boolean isPluginTool(ItemStack tool) {
        if (tool.getType() == Material.AIR || !tool.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = tool.getItemMeta();
        return meta.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    private boolean canDestroy(ItemStack tool, Block block) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return false;
        }
        var canDestroy = meta.getCanDestroy();
        return canDestroy != null && canDestroy.contains(block.getType());

    }

}
