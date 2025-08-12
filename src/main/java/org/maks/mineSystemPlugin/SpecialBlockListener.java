package org.maks.mineSystemPlugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.maks.mineSystemPlugin.item.CustomItems;
import org.maks.mineSystemPlugin.tool.CustomTool;

import java.util.*;

public class SpecialBlockListener implements Listener {
    private final MineSystemPlugin plugin;
    private final Map<Location, Integer> hitMap = new HashMap<>();
    private final Map<Location, ArmorStand> holograms = new HashMap<>();
    private final Map<Location, BukkitTask> hideTasks = new HashMap<>();
    private final Random random = new Random();

    public SpecialBlockListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        Location loc = block.getLocation();
        if (!plugin.getSphereManager().isInsideSphere(loc)) {
            event.setCancelled(true);
            return;
        }

        int requiredHits;
        int interval;
        String display;
        switch (type) {
            case MOSS_BLOCK -> { requiredHits = 75; interval = 25; display = "Moss Block"; }
            case BONE_BLOCK -> { requiredHits = 100; interval = 25; display = "Bone Block"; }
            case AMETHYST_BLOCK -> { requiredHits = 25; interval = 5; display = "Crystals"; }
            default -> { return; }
        }
        int hits = hitMap.getOrDefault(loc, 0) + 1;
        int remaining = requiredHits - hits;
        hitMap.put(loc, hits);

        showHologram(loc, display, remaining, requiredHits);

        event.setCancelled(true);

        if (hits < requiredHits) {
            if (hits % interval == 0) {
                int amount = random.nextInt(4) + 1;
                for (int i = 0; i < amount; i++) {
                    if (type == Material.MOSS_BLOCK) {
                        block.getWorld().dropItemNaturally(loc, createLeaf(randomTier()));
                    } else if (type == Material.BONE_BLOCK) {
                        block.getWorld().dropItemNaturally(loc, createBone(randomTier()));
                    } else {
                        block.getWorld().dropItemNaturally(loc, createCrystal());
                    }
                }
            }
            return;
        }

        hitMap.remove(loc);
        block.setType(Material.AIR);
        removeHologram(loc);
        World world = block.getWorld();

        // handle duplication enchant
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        int dupLevel = CustomTool.getDuplicateLevel(tool, plugin);
        double chance = switch (dupLevel) {
            case 1 -> 0.03;
            case 2 -> 0.04;
            case 3 -> 0.05;
            default -> 0.0;
        };
        boolean duplicate = Math.random() < chance;

        if (type == Material.MOSS_BLOCK) {
            ItemStack leaf = createLeaf(randomTier());
            world.dropItemNaturally(loc, leaf);
            if (duplicate) {
                world.dropItemNaturally(loc, leaf.clone());
            }
        } else if (type == Material.BONE_BLOCK) {
            ItemStack bone = createBone(randomTier());
            world.dropItemNaturally(loc, bone);
            if (duplicate) {
                world.dropItemNaturally(loc, bone.clone());
            }
        } else if (type == Material.AMETHYST_BLOCK) {
            List<ItemStack> crystals = createCrystals();
            for (ItemStack stack : crystals) {
                world.dropItemNaturally(loc, stack);
                if (duplicate) {
                    world.dropItemNaturally(loc, stack.clone());
                }
            }
        }
    }

    private void showHologram(Location loc, String name, int remaining, int max) {
        ArmorStand stand = holograms.computeIfAbsent(loc, l ->
                loc.getWorld().spawn(loc.clone().add(0.5, 1.2, 0.5), ArmorStand.class, as -> {
                    as.setInvisible(true);
                    as.setMarker(true);
                    as.setGravity(false);
                })
        );
        if (remaining <= 0) {
            return;
        }
        stand.setCustomName(formatName(name, remaining, max));
        stand.setCustomNameVisible(true);
        BukkitTask task = hideTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }
        hideTasks.put(loc, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ArmorStand as = holograms.get(loc);
            if (as != null) {
                as.setCustomNameVisible(false);
            }
            hideTasks.remove(loc);
        }, 60L));
    }

    private void removeHologram(Location loc) {
        ArmorStand stand = holograms.remove(loc);
        if (stand != null) {
            stand.remove();
        }
        BukkitTask task = hideTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }
    }

    private String formatName(String name, int remaining, int max) {
        return ChatColor.YELLOW + name + " " + ChatColor.RED + remaining + "/" + max + " " + progressBar(remaining, max);
    }

    private String progressBar(int remaining, int max) {
        int bars = 10;
        int filled = (int) Math.round((double) remaining / max * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? ChatColor.GREEN : ChatColor.DARK_GRAY).append("|");
        }
        return sb.toString();
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

    private String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }
}
