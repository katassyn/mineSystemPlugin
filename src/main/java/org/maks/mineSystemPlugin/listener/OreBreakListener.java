package org.maks.mineSystemPlugin.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.MineSystemPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class OreBreakListener implements Listener {

    private static final List<Material> ORE_REWARDS =
            Arrays.asList(Material.COAL, Material.IRON_INGOT, Material.DIAMOND);

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

        Player player = event.getPlayer();
        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
        event.setDropItems(false);

        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        int total = plugin.incrementOreCount();
        if (total % 20 == 0) {
            Material rewardMat = ORE_REWARDS.get(random.nextInt(ORE_REWARDS.size()));
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(rewardMat));
        }
    }
}

