package org.maks.mineSystemPlugin.menu;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import java.lang.reflect.Method;
import java.util.Set;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.item.CustomItems;

/**
 * GUI allowing players to sell mined ores for Vault currency.
 */
public class SellMenu implements InventoryHolder, Listener {
    private final JavaPlugin plugin;
    private final Economy economy;
    private final Inventory inventory;
    private final ItemStack sellButton;
    private final ItemStack fillerItem;
    private static final int SELL_SLOT = 22;
    private static final Set<Integer> FILLER_SLOTS = Set.of(18, 19, 20, 21, 23, 24, 25, 26);

    public SellMenu(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.GOLD + "Sell Ores");
        this.sellButton = createSellButton();
        this.fillerItem = createFillerItem();
        for (int slot : FILLER_SLOTS) {
            inventory.setItem(slot, fillerItem);
        }
        inventory.setItem(SELL_SLOT, sellButton);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createSellButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Sell");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private boolean isOre(Material mat) {
        return mat.name().endsWith("_ORE");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() == inventory) {
            int slot = event.getSlot();
            if (slot == SELL_SLOT) {
                event.setCancelled(true);
                sellItems((Player) event.getWhoClicked());
                return;
            }
            if (FILLER_SLOTS.contains(slot)) {
                event.setCancelled(true);
                return;
            }
            if (item != null && !isOre(item.getType())) {
                event.setCancelled(true);
            }
        } else if (event.isShiftClick()) {
            if (item != null && !isOre(item.getType())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && !isOre(item.getType())) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getRawSlots().contains(SELL_SLOT) ||
            event.getRawSlots().stream().anyMatch(FILLER_SLOTS::contains)) {
            event.setCancelled(true);
        }
    }

    private void sellItems(Player player) {
        double total = 0;
        String base = player.getWorld().getName();
        boolean minerDayActive = isMinerDayActive();
        double multiplier = minerDayActive ? 2.0 : 1.0;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == SELL_SLOT || FILLER_SLOTS.contains(i)) {
                continue;
            }
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            Material mat = item.getType();
            if (!isOre(mat)) {
                continue;
            }
            String oreId = CustomItems.getId(item);
            double price;
            if (oreId != null) {
                price = plugin.getConfig().getDouble("sell-prices." + base + "." + oreId, 0.0);
            } else {
                price = plugin.getConfig().getDouble("sell-prices." + base + "." + mat.name(), 0.0);
            }
            total += price * item.getAmount() * multiplier;
            inventory.setItem(i, null);
        }
        inventory.setItem(SELL_SLOT, sellButton);
        if (total > 0) {
            economy.depositPlayer(player, total);
            if (minerDayActive) {
                player.sendMessage(ChatColor.GREEN + "Sold ores for $" + String.format("%.0f", total) + " " + ChatColor.GOLD + "(x2 Miner Day bonus!)");
            } else {
                player.sendMessage(ChatColor.GREEN + "Sold ores for $" + String.format("%.0f", total));
            }
        } else {
            player.sendMessage(ChatColor.RED + "No ores to sell");
        }
    }

    /**
     * Check if miner_day event is active using EventPlugin API.
     */
    private boolean isMinerDayActive() {
        try {
            Class<?> apiClass = Class.forName("org.maks.eventPlugin.api.EventPluginAPI");
            Method isActiveMethod = apiClass.getMethod("isEventActive", String.class);
            return (boolean) isActiveMethod.invoke(null, "miner_day");
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        Player player = (Player) event.getPlayer();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == SELL_SLOT || FILLER_SLOTS.contains(i)) {
                continue;
            }
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
            }
        }
        HandlerList.unregisterAll(this);
    }
}
