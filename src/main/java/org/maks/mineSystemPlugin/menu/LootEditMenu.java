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
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.repository.LootRepository;

import java.util.List;
import java.util.Map;

/**
 * Inventory GUI for editing loot items and their chances.
 */
public class LootEditMenu implements InventoryHolder, Listener {
    private final JavaPlugin plugin;
    private final LootRepository storage;
    private final LootManager lootManager;
    private final Inventory inventory;

    public LootEditMenu(JavaPlugin plugin, LootRepository storage, LootManager lootManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.lootManager = lootManager;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GREEN + "Loot Editor");

        Map<Material, Integer> items = storage.load().join();
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            ItemStack item = new ItemStack(entry.getKey());
            ItemMeta meta = item.getItemMeta();
            meta.setLore(List.of(ChatColor.YELLOW + "Chance: " + entry.getValue() + "%"));
            item.setItemMeta(meta);
            inventory.addItem(item);
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        if (event.getClickedInventory() != inventory) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        event.setCancelled(true);
        ItemMeta meta = item.getItemMeta();
        int chance = parseChance(meta);
        if (event.isLeftClick()) {
            chance = Math.min(100, chance + 1);
        } else if (event.isRightClick()) {
            chance = Math.max(0, chance - 1);
        }
        meta.setLore(List.of(ChatColor.YELLOW + "Chance: " + chance + "%"));
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        HandlerList.unregisterAll(this);
        Map<Material, Integer> map = new HashMap<>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            int chance = parseChance(item.getItemMeta());
            if (chance > 0) {
                map.put(item.getType(), chance);
            }
        }
        storage.save(map);
        lootManager.setProbabilities(map);
    }

    private int parseChance(ItemMeta meta) {
        if (meta == null || meta.getLore() == null || meta.getLore().isEmpty()) {
            return 0;
        }
        String line = ChatColor.stripColor(meta.getLore().get(0));
        if (line.startsWith("Chance: ")) {
            try {
                return Integer.parseInt(line.substring(8, line.length() - 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
