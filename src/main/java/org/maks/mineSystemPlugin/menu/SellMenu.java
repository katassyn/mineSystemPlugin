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
    private static final int SELL_SLOT = 26;

    public SellMenu(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.GOLD + "Sell Ores");
        this.sellButton = createSellButton();
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
        if (event.getSlot() == SELL_SLOT && event.getClickedInventory() == inventory) {
            event.setCancelled(true);
            sellItems((Player) event.getWhoClicked());
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() == inventory) {
            if (event.getSlot() == SELL_SLOT) {
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
        if (event.getRawSlots().contains(SELL_SLOT)) {
            event.setCancelled(true);
        }
    }

    private void sellItems(Player player) {
        double total = 0;
        String base = player.getWorld().getName();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == SELL_SLOT) {
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
            total += price * item.getAmount();
            inventory.setItem(i, null);
        }
        inventory.setItem(SELL_SLOT, sellButton);
        if (total > 0) {
            economy.depositPlayer(player, total);
            player.sendMessage(ChatColor.GREEN + "Sold ores for $" + total);
        } else {
            player.sendMessage(ChatColor.RED + "No ores to sell");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        Player player = (Player) event.getPlayer();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == SELL_SLOT) {
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
