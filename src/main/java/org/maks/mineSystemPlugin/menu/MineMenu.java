package org.maks.mineSystemPlugin.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

/**
 * GUI for selecting between the regular and premium mines.
 * Selecting the premium mine consumes a Premium Mine Voucher
 * from the player's inventory (ignoring color codes).
 */
public class MineMenu implements InventoryHolder, Listener {
    private final JavaPlugin plugin;
    private final Inventory inventory;

    public MineMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 9, ChatColor.DARK_AQUA + "Choose Mine");

        ItemStack normal = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta normalMeta = normal.getItemMeta();
        if (normalMeta != null) {
            normalMeta.setDisplayName(ChatColor.GRAY + "Regular Mine");
            normal.setItemMeta(normalMeta);
        }
        inventory.setItem(3, normal);

        ItemStack premium = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta premiumMeta = premium.getItemMeta();
        if (premiumMeta != null) {
            premiumMeta.setDisplayName(ChatColor.AQUA + "Premium Mine");
            premium.setItemMeta(premiumMeta);
        }
        inventory.setItem(5, premium);

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
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (name.equalsIgnoreCase("Regular Mine")) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Entering the regular mine...");
            // Teleportation to the regular mine would occur here.
        } else if (name.equalsIgnoreCase("Premium Mine")) {
            player.closeInventory();
            if (consumeVoucher(player)) {
                player.sendMessage(ChatColor.GREEN + "Voucher redeemed. Entering the premium mine...");
                // Teleportation to the premium mine would occur here.
            } else {
                player.sendMessage(ChatColor.RED + "You need a Premium Mine Voucher to enter!");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }

    private boolean consumeVoucher(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() != Material.POTATO) {
                continue;
            }
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("Premium Mine Voucher")) {
                int amount = stack.getAmount();
                if (amount > 1) {
                    stack.setAmount(amount - 1);
                } else {
                    inventory.setItem(slot, null);
                }
                return true;
            }
        }
        return false;
    }
}
