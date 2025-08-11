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
import org.maks.mineSystemPlugin.item.CustomItems;
import org.maks.mineSystemPlugin.tool.CustomTool;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class OreBreakListener implements Listener {

    private static final List<String> BONUS_ITEMS =
            Arrays.asList("ore_I", "ore_II", "ore_III");

    private final MineSystemPlugin plugin;
    private final Random random = new Random();

    public OreBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        if (!type.toString().endsWith("_ORE")) {
            return;
        }

        if (!plugin.getSphereManager().isInsideSphere(block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops = block.getDrops(tool);
        event.setDropItems(false);

        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        int amount = drops.stream().mapToInt(ItemStack::getAmount).sum();
        int pickaxeLevel = CustomTool.getToolLevel(tool);
        Bukkit.getPluginManager().callEvent(new OreMinedEvent(player, type, amount, pickaxeLevel));

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
    }
}

