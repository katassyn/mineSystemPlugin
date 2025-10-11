package org.maks.mineSystemPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import me.clip.placeholderapi.PlaceholderAPI;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.events.OreMinedEvent;
import org.maks.mineSystemPlugin.tool.CustomTool;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class OreBreakListener implements Listener {

    private final MineSystemPlugin plugin;

    // Special blocks that have their own custom drop logic in SpecialBlockListener
    private static final Set<Material> SPECIAL_BLOCKS = EnumSet.of(
            Material.AMETHYST_BLOCK,
            Material.MOSS_BLOCK,
            Material.BONE_BLOCK
    );

    public OreBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        // Skip special blocks - they're handled by SpecialBlockListener
        if (SPECIAL_BLOCKS.contains(type)) {
            return;
        }

        if (!type.toString().endsWith("_ORE")) {
            return;
        }

        if (plugin.consumePlayerPlaced(block.getLocation())) {
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

        // Check SKELETON pet sphere doubling chance
        double sphereDoubleChance = getPetSphereDoubleChance(player);
        boolean shouldDouble = sphereDoubleChance > 0 && Math.random() * 100 < sphereDoubleChance;

        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            // SKELETON pet effect: double sphere drops
            if (shouldDouble) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }

        if (shouldDouble) {
            player.sendMessage("§d✦ SKELETON PET EFFECT: Double ore drop!");
        }

        int amount = drops.stream().mapToInt(ItemStack::getAmount).sum();
        int pickaxeLevel = CustomTool.getToolLevel(tool);
        Bukkit.getPluginManager().callEvent(new OreMinedEvent(player, type, amount, pickaxeLevel));

        int total = plugin.incrementOreCount(player.getUniqueId());
        if (total % 20 == 0) {
            plugin.dropRandomOreReward(player, block.getLocation());
        }
    }

    /**
     * Get SKELETON pet sphere double chance percentage
     */
    private double getPetSphereDoubleChance(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return 0.0;
        }

        String placeholder = PlaceholderAPI.setPlaceholders(player, "%petplugin_sphere_double_chance%");
        try {
            return Double.parseDouble(placeholder);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}