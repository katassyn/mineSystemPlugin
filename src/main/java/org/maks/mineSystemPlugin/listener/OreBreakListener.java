package org.maks.mineSystemPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.events.OreMinedEvent;
import org.maks.mineSystemPlugin.tool.CustomTool;

import java.util.Collection;

public class OreBreakListener implements Listener {

    private final MineSystemPlugin plugin;

    public OreBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        if (!type.toString().endsWith("_ORE")) {
            return;
        }

        if (plugin.isCustomOre(type)) {
            return; // handled by BlockBreakListener
        }

        Player player = event.getPlayer();
        boolean bypass = player.isOp() || player.hasPermission("minesystem.admin");
        if (!bypass && !plugin.getSphereManager().isInsideSphere(block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();

        Collection<ItemStack> drops = block.getDrops(tool);
        event.setDropItems(false);

        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        int amount = drops.stream().mapToInt(ItemStack::getAmount).sum();
        int pickaxeLevel = CustomTool.getToolLevel(tool);
        Bukkit.getPluginManager().callEvent(new OreMinedEvent(player, type, amount, pickaxeLevel));

        int total = plugin.incrementOreCount(player.getUniqueId());
        if (total % 20 == 0) {
            plugin.dropRandomOreReward(player, block.getLocation());

        }
    }
}

