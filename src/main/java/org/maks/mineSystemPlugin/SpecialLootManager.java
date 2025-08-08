package org.maks.mineSystemPlugin;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Holds fixed loot tables per schematic for special zones.
 */
public class SpecialLootManager {
    private final Map<String, Map<Material, SpecialLootEntry>> tables = new HashMap<>();
    private final Random random = new Random();

    public void setLoot(String schematic, Map<Material, SpecialLootEntry> items) {
        tables.put(schematic, items);
    }

    public Map<Material, SpecialLootEntry> getLoot(String schematic) {
        return tables.get(schematic);
    }

    /**
     * Fills inventory with items according to configured chances for the given schematic.
     */
    public void fillInventory(String schematic, Inventory inventory) {
        Map<Material, SpecialLootEntry> items = tables.get(schematic);
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        int i = 0;
        for (Map.Entry<Material, SpecialLootEntry> entry : items.entrySet()) {
            if (i >= slots.size()) {
                break;
            }
            SpecialLootEntry spec = entry.getValue();
            if (random.nextInt(100) < spec.chance()) {
                inventory.setItem(slots.get(i), new ItemStack(entry.getKey(), spec.amount()));
                i++;
            }
        }
    }
}
