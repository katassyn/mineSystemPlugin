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
import org.maks.mineSystemPlugin.sphere.SphereManager;

/**
 * GUI for purchasing mining spheres. Players may spawn a regular sphere
 * by spending stamina or a premium sphere by redeeming a voucher.
 */
public class MineMenu implements InventoryHolder, Listener {
    private final JavaPlugin plugin;
    private final SphereManager sphereManager;
    private final Inventory inventory;

    public MineMenu(JavaPlugin plugin, SphereManager sphereManager) {
        this.plugin = plugin;
        this.sphereManager = sphereManager;
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
            if (sphereManager.createSphere(player, false)) {
                player.sendMessage(ChatColor.GREEN + "Sphere created!");
            }
        } else if (name.equalsIgnoreCase("Premium Mine")) {
            player.closeInventory();
            if (consumeVoucher(player)) {
                if (sphereManager.createSphere(player, true)) {
                    player.sendMessage(ChatColor.GREEN + "Premium sphere created!");
                }
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
