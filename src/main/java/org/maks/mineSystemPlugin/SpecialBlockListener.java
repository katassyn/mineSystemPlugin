package org.maks.mineSystemPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.item.CustomItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SpecialBlockListener implements Listener {
    private final Map<Location, Integer> hitMap = new HashMap<>();
    private final Random random = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        int requiredHits;
        switch (type) {
            case MOSS_BLOCK -> requiredHits = 75;
            case BONE_BLOCK -> requiredHits = 100;
            case AMETHYST_BLOCK -> requiredHits = 25;
            default -> { return; }
        }

        Location location = block.getLocation();
        int hits = hitMap.getOrDefault(location, 0) + 1;

        if (hits < requiredHits) {
            hitMap.put(location, hits);
            event.setCancelled(true);
            return;
        }

        hitMap.remove(location);
        event.setCancelled(true);
        block.setType(Material.AIR);
        World world = block.getWorld();

        if (type == Material.MOSS_BLOCK) {
            world.dropItemNaturally(location, createLeaf(randomTier()));
        } else if (type == Material.BONE_BLOCK) {
            world.dropItemNaturally(location, createBone(randomTier()));
        } else if (type == Material.AMETHYST_BLOCK) {
            for (ItemStack stack : createCrystals()) {
                world.dropItemNaturally(location, stack);
            }
        }
    }

    private int randomTier() {
        return random.nextInt(3) + 1; // 1-3
    }

    private ItemStack createLeaf(int tier) {
        String id = "leaf_" + roman(tier);
        ItemStack item = CustomItems.get(id);
        return item != null ? item : new ItemStack(Material.OAK_LEAVES);
    }

    private ItemStack createBone(int tier) {
        String id = "bone_" + roman(tier);
        ItemStack item = CustomItems.get(id);
        return item != null ? item : new ItemStack(Material.BONE);
    }

    private ItemStack createCrystal() {
        ItemStack item = CustomItems.get("Crystal");
        return item != null ? item : new ItemStack(Material.BRICK);
    }

    private String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }

    private List<ItemStack> createCrystals() {
        int amount = random.nextInt(3) + 1; // 1-3
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            list.add(createCrystal());
            if (random.nextBoolean()) {
                list.add(createCrystal());
            }
        }
        return list;
    }
}
