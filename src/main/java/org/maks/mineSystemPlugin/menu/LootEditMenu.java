package org.maks.mineSystemPlugin.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.LootEntry;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.repository.LootRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory GUI for editing loot items. Players can add items by
 * moving them into the top inventory, adjust chances with clicks and
 * save/cancel their changes.
 */
public class LootEditMenu implements InventoryHolder, Listener {
    private static final int INVENTORY_SIZE = 54;
    private static final int SAVE_SLOT = 45;
    private static final int CANCEL_SLOT = 53;

    private final JavaPlugin plugin;
    private final LootRepository storage;
    private final LootManager lootManager;
    private final Inventory inventory;
    private final NamespacedKey chanceKey;
    private boolean cancelClicked = false;
    private boolean saveClicked = false;

    public LootEditMenu(JavaPlugin plugin, LootRepository storage, LootManager lootManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.lootManager = lootManager;
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, ChatColor.DARK_GREEN + "Loot Editor");
        this.chanceKey = new NamespacedKey(plugin, "chance");

        // Load existing items
        List<LootEntry> items = storage.load().join();
        int slot = 0;
        for (LootEntry e : items) {
            if (slot >= 45) break;
            ItemStack item = e.item().clone();
            setChance(item, e.chance());
            inventory.setItem(slot++, item);
        }

        addActionButtons();

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void addActionButtons() {
        ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName(ChatColor.GREEN + "Save Changes");
            save.setItemMeta(saveMeta);
        }
        inventory.setItem(SAVE_SLOT, save);

        ItemStack cancel = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(CANCEL_SLOT, cancel);
    }

    private void setChance(ItemStack item, int chance) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Chance:"));
        lore.add(ChatColor.GRAY + "Chance: " + ChatColor.YELLOW + chance + "%");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(chanceKey, PersistentDataType.INTEGER, chance);
        item.setItemMeta(meta);
    }

    private int getChance(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(chanceKey, PersistentDataType.INTEGER, 0);
    }

    private void updateChance(ItemStack item, int delta) {
        int chance = Math.max(0, Math.min(100, getChance(item) + delta));
        setChance(item, chance);
    }

    private ItemStack stripChance(ItemStack item) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(chanceKey);
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>(meta.getLore());
                lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Chance:"));
                meta.setLore(lore.isEmpty() ? null : lore);
            }
            clone.setItemMeta(meta);
        }
        return clone;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }

        int slot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();

        // Clicks in top inventory
        if (slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);

            if (slot == SAVE_SLOT) {
                saveClicked = true;
                saveChanges();
                event.getWhoClicked().closeInventory();
                return;
            }

            if (slot == CANCEL_SLOT) {
                cancelClicked = true;
                event.getWhoClicked().closeInventory();
                return;
            }

            // Item slots
            if (slot < 45) {
                // Empty slot – add new item from cursor
                if ((current == null || current.getType() == Material.AIR)) {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        ItemStack item = cursor.clone();
                        setChance(item, 50);
                        inventory.setItem(slot, item);
                        event.getView().setCursor(null);
                    }
                    return;
                }

                switch (event.getClick()) {
                    case LEFT -> updateChance(current, 10);
                    case RIGHT -> updateChance(current, -10);
                    case SHIFT_LEFT -> updateChance(current, 1);
                    case SHIFT_RIGHT -> updateChance(current, -1);
                    case MIDDLE -> inventory.setItem(slot, null);
                    default -> {
                    }
                }
            }
            return;
        }

        // Click in player's inventory
        if ((event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) && current != null && current.getType() != Material.AIR) {
            event.setCancelled(true);
            for (int i = 0; i < 45; i++) {
                if (inventory.getItem(i) == null) {
                    ItemStack item = current.clone();
                    setChance(item, 50);
                    inventory.setItem(i, item);
                    current.setAmount(0);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                if (slot >= 45) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void saveChanges() {
        List<LootEntry> list = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            int chance = getChance(item);
            ItemStack clean = stripChance(item);
            list.add(new LootEntry(clean, chance));
        }
        storage.save(list);
        lootManager.setEntries(list);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        HandlerList.unregisterAll(this);
        if (!cancelClicked && !saveClicked) {
            saveChanges();
        }
    }
}

