package org.maks.mineSystemPlugin.tool;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

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

    // Fallback list of blocks that any plugin pickaxe is allowed to break
    // inside a sphere. This mirrors the CanBreak lists defined for the tools in
    // itemy.md so durability can still update even if the underlying item
    // metadata lacks CanDestroy entries.
    private static final Set<Material> DEFAULT_CAN_DESTROY = EnumSet.of(
            Material.COAL_ORE,
            Material.IRON_ORE,
            Material.LAPIS_ORE,
            Material.REDSTONE_ORE,
            Material.GOLD_ORE,
            Material.EMERALD_ORE,
            Material.DIAMOND_ORE,
            Material.AMETHYST_BLOCK,
            Material.MOSS_BLOCK,
            Material.BONE_BLOCK
    );

    public ToolListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
        // Always enable debug output so durability problems can be traced even if
        // the configuration flag is missing or set incorrectly.
        this.debug = true;
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

        // initialise durability and marker metadata before any checks and ensure
        // the mutated ItemStack is written back to the inventory so further
        // operations see the updated persistent data.
        CustomTool.ensureDurability(tool, plugin);
        player.getInventory().setItemInMainHand(tool);


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
                var metaCanDestroy = meta.getCanDestroy();
                plugin.getLogger().info("[ToolListener] canDestroy=" + metaCanDestroy
                        + (metaCanDestroy == null || metaCanDestroy.isEmpty() ? " (using defaults)" : ""));

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
            // write back updated durability to the player's inventory
            player.getInventory().setItemInMainHand(tool);

        }
        player.updateInventory();
        if (debug) {
            plugin.getLogger().info("[ToolListener] Final state: " + (event.isCancelled() ? "cancelled" : "allowed"));
        }
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
        Material type = block.getType();
        if (meta != null) {
            var canDestroy = meta.getCanDestroy();
            if (canDestroy != null && !canDestroy.isEmpty()) {
                return canDestroy.contains(type);
            }
        }
        // If the item metadata does not expose a CanDestroy list, fall back to
        // the predefined set so that configured pickaxes still function.
        return DEFAULT_CAN_DESTROY.contains(type);

    }

}
