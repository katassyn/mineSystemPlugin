package org.maks.mineSystemPlugin;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

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
        ItemStack item = new ItemStack(Material.OAK_LEAVES);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("leaf_" + tier);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBone(int tier) {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("bone_" + tier);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCrystal() {
        ItemStack item = new ItemStack(Material.BRICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Crystal");
            item.setItemMeta(meta);
        }
        return item;
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
