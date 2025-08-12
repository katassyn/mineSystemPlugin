package org.maks.mineSystemPlugin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Handles randomisation logic for loot rolls.
 */
public class LootManager {
    private final Random random = new Random();
    private List<LootEntry> entries = new ArrayList<>();

    /**
     * Updates internal loot entries.
     */
    public void setEntries(List<LootEntry> entries) {
        this.entries = entries;
    }

    // Probability distribution for number of rewards in a chest, index 0 corresponds to count=1
    private static final double[] COUNT_DISTRIBUTION = {
            0.03, 0.08, 0.20, 0.46, 0.95, 1.79, 3.05, 4.90, 7.44,
            10.51, 13.72, 16.34, 18.02, 18.90, 18.02, 16.34, 13.72,
            10.51, 7.44, 4.90, 3.05, 1.79, 0.95, 0.46, 0.20, 0.08,
            0.03
    };

    /**
     * Rolls number of items using predefined probability distribution.
     */
    public int rollItemCount() {
        double roll = random.nextDouble() * 100.0;
        double cumulative = 0;
        for (int i = 0; i < COUNT_DISTRIBUTION.length; i++) {
            cumulative += COUNT_DISTRIBUTION[i];
            if (roll <= cumulative) {
                return i + 1;
            }
        }
        return COUNT_DISTRIBUTION.length; // fallback
    }

    /**
     * Selects an item from configured probabilities.
     */
    public ItemStack rollItem() {
        if (entries.isEmpty()) {
            return null;
        }
        int total = entries.stream().mapToInt(LootEntry::chance).sum();
        if (total <= 0) {
            return null;
        }
        int r = random.nextInt(total);
        int cumulative = 0;
        for (LootEntry entry : entries) {
            cumulative += entry.chance();
            if (r < cumulative) {
                return entry.item().clone();
            }
        }
        return entries.get(0).item().clone();
    }

    /**
     * Fills provided inventory with randomly selected items according to configured probabilities.
     * Items are placed into random empty slots.
     */
    public void fillInventory(Inventory inventory) {
        int count = rollItemCount();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        for (int i = 0; i < count && i < slots.size(); i++) {
            ItemStack item = rollItem();
            if (item != null) {
                inventory.setItem(slots.get(i), item);
            }
        }
    }
}
