package org.maks.mineSystemPlugin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Holds fixed loot tables per schematic for special zones.
 */
public class SpecialLootManager {
    private final Map<String, List<SpecialLootEntry>> tables = new HashMap<>();
    private final Random random = new Random();

    public void setLoot(String schematic, List<SpecialLootEntry> items) {
        tables.put(schematic, items);
    }

    public List<SpecialLootEntry> getLoot(String schematic) {
        return tables.get(schematic);
    }

    /**
     * Fills inventory with items according to configured chances for the given schematic.
     */
    public void fillInventory(String schematic, Inventory inventory) {
        List<SpecialLootEntry> items = tables.get(schematic);
        if (items == null || items.isEmpty()) {
            return;
        }
        int total = items.stream().mapToInt(SpecialLootEntry::chance).sum();
        double scale = total > 100 ? 100.0 / total : 1.0;

        List<Integer> slots = IntStream.range(0, inventory.getSize())
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(slots, random);

        int toFill = Math.max(3, Math.min(inventory.getSize(), items.size()));
        double weightSum = items.stream().mapToDouble(e -> e.chance() * scale).sum();

        for (int i = 0; i < toFill; i++) {
            double r = random.nextDouble() * weightSum;
            double cumulative = 0;
            for (SpecialLootEntry entry : items) {
                cumulative += entry.chance() * scale;
                if (r <= cumulative) {
                    inventory.setItem(slots.get(i), entry.item().clone());
                    break;
                }

            }
        }
    }
}
