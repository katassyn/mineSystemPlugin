package org.maks.mineSystemPlugin.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.maks.mineSystemPlugin.SpecialLootEntry;
import org.maks.mineSystemPlugin.SpecialLootManager;
import org.maks.mineSystemPlugin.repository.SpecialLootRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * GUI for configuring fixed loot of special schematics.
 */
public class SpecialLootMenu implements InventoryHolder, Listener {
    private final JavaPlugin plugin;
    private final String schematic;
    private final SpecialLootRepository storage;
    private final SpecialLootManager manager;
    private final Inventory inventory;
    private final NamespacedKey chanceKey;

    public SpecialLootMenu(JavaPlugin plugin, String schematic, SpecialLootRepository storage, SpecialLootManager manager) {
        this.plugin = plugin;
        this.schematic = schematic;
        this.storage = storage;
        this.manager = manager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_PURPLE + "Special Loot: " + schematic);
        this.chanceKey = new NamespacedKey(plugin, "chance");
        Map<Material, SpecialLootEntry> existing = manager.getLoot(schematic);
        if (existing != null) {
            for (Map.Entry<Material, SpecialLootEntry> e : existing.entrySet()) {
                ItemStack item = new ItemStack(e.getKey(), e.getValue().amount());
                setChance(item, e.getValue().chance());
                inventory.addItem(item);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void setChance(ItemStack item, int chance) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.setLore(java.util.List.of(ChatColor.GRAY + "Chance: " + chance + "%"));
        meta.getPersistentDataContainer().set(chanceKey, PersistentDataType.INTEGER, chance);
        item.setItemMeta(meta);
    }

    private int getChance(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(chanceKey, PersistentDataType.INTEGER, 0);
    }

    private void updateChance(ItemStack item, int delta) {
        int chance = Math.max(0, Math.min(100, getChance(item) + delta));
        setChance(item, chance);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        switch (event.getClick()) {
            case LEFT -> {
                updateChance(item, 10);
                event.setCancelled(true);
            }
            case RIGHT -> {
                updateChance(item, -10);
                event.setCancelled(true);
            }
            case SHIFT_LEFT -> {
                updateChance(item, 1);
                event.setCancelled(true);
            }
            case SHIFT_RIGHT -> {
                updateChance(item, -1);
                event.setCancelled(true);
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        HandlerList.unregisterAll(this);
        Map<Material, SpecialLootEntry> map = new HashMap<>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            int chance = getChance(item);
            int amount = item.getAmount();
            map.put(item.getType(), new SpecialLootEntry(amount, chance));
        }
        storage.save(schematic, map);
        manager.setLoot(schematic, map);
    }
}
