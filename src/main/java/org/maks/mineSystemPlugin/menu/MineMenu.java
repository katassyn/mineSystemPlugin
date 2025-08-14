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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.PermissionAttachment;
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
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_AQUA + "Choose Mine");

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        ItemStack normal = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta normalMeta = normal.getItemMeta();
        if (normalMeta != null) {
            normalMeta.setDisplayName(ChatColor.GRAY + "Regular Mine");
            normalMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            normal.setItemMeta(normalMeta);
        }
        inventory.setItem(12, normal);

        ItemStack premium = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta premiumMeta = premium.getItemMeta();
        if (premiumMeta != null) {
            premiumMeta.setDisplayName(ChatColor.AQUA + "Premium Mine");
            premiumMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            premium.setItemMeta(premiumMeta);
        }
        inventory.setItem(14, premium);

        ItemStack shop = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shop.getItemMeta();
        if (shopMeta != null) {
            shopMeta.setDisplayName(ChatColor.GREEN + "Mine Shop");
            shop.setItemMeta(shopMeta);
        }
        inventory.setItem(10, shop);

        ItemStack sell = new ItemStack(Material.CHEST);
        ItemMeta sellMeta = sell.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName(ChatColor.GOLD + "Sell Ores");
            sell.setItemMeta(sellMeta);
        }
        inventory.setItem(16, sell);

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
            if (sphereManager.createSphere(player, false, "MineMenu")) {
                player.sendMessage(ChatColor.GREEN + "Sphere created!");
            }
        } else if (name.equalsIgnoreCase("Premium Mine")) {
            player.closeInventory();
            if (consumeVoucher(player)) {
                if (sphereManager.createSphere(player, true, "MineMenu")) {
                    player.sendMessage(ChatColor.GREEN + "Premium sphere created!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "You need a Premium Mine Voucher to enter!");
            }
        } else if (name.equalsIgnoreCase("Sell Ores")) {
            player.closeInventory();
            if (plugin instanceof org.maks.mineSystemPlugin.MineSystemPlugin main) {
                if (main.getEconomy() != null) {
                    SellMenu menu = new SellMenu(plugin, main.getEconomy());
                    player.openInventory(menu.getInventory());
                } else {
                    player.sendMessage(ChatColor.RED + "Economy unavailable");
                }
            }
        } else if (name.equalsIgnoreCase("Mine Shop")) {
            player.closeInventory();
            if (!player.hasPermission("mycraftingplugin.use")) {
                PermissionAttachment attachment = player.addAttachment(plugin, "mycraftingplugin.use", true);
                player.performCommand("mine_shop");
                attachment.remove();
            } else {
                player.performCommand("mine_shop");
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
