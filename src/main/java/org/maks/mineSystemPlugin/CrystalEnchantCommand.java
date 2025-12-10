package org.maks.mineSystemPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import org.maks.mineSystemPlugin.CrystalCurrency;

import org.maks.mineSystemPlugin.tool.CustomTool;

public class CrystalEnchantCommand implements CommandExecutor, Listener {

    private final Random random = new Random();
    private final Plugin plugin;

    public CrystalEnchantCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !item.getType().name().endsWith("_PICKAXE")) {
            player.sendMessage(ChatColor.RED + "Hold a pickaxe in your main hand.");
            return true;
        }

        if (!item.getEnchantments().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Pickaxe already has enchantments.");
            return true;
        }

        int baseCost = getBaseCost(item.getType());
        if (baseCost <= 0) {
            player.sendMessage(ChatColor.RED + "You must hold a valid pickaxe.");
            return true;
        }

        openSelectionMenu(player, item, baseCost);
        return true;
    }

    /**
     * Opens the initial menu where player chooses between Basic and Powerful enchanting.
     */
    private void openSelectionMenu(Player player, ItemStack tool, int baseCost) {
        Inventory inv = Bukkit.createInventory(new EnchantSelectionMenu(tool, baseCost), 27, ChatColor.DARK_AQUA + "Choose Enchanting Type");

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Basic Enchanting (tiers 1-3)
        ItemStack basicBook = new ItemStack(Material.BOOK);
        ItemMeta basicMeta = basicBook.getItemMeta();
        basicMeta.setDisplayName(ChatColor.GREEN + "Basic Enchanting");
        List<String> basicLore = new ArrayList<>();
        basicLore.add(ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + baseCost + " crystals");
        basicLore.add("");
        basicLore.add(ChatColor.GRAY + "Enchant tiers: " + ChatColor.WHITE + "I - III");
        basicLore.add(ChatColor.GRAY + "Tier chances:");
        basicLore.add(ChatColor.GRAY + "  • Tier I: " + ChatColor.WHITE + "70%");
        basicLore.add(ChatColor.GRAY + "  • Tier II: " + ChatColor.WHITE + "25%");
        basicLore.add(ChatColor.GRAY + "  • Tier III: " + ChatColor.WHITE + "5%");
        basicLore.add("");
        basicLore.add(ChatColor.YELLOW + "Click to select");
        basicMeta.setLore(basicLore);
        basicBook.setItemMeta(basicMeta);
        inv.setItem(11, basicBook);

        // Powerful Enchanting (tiers 3-5)
        ItemStack powerfulBook = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta powerfulMeta = powerfulBook.getItemMeta();
        powerfulMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Powerful Enchanting");
        int powerfulCost = baseCost * 10;
        List<String> powerfulLore = new ArrayList<>();
        powerfulLore.add(ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + powerfulCost + " crystals");
        powerfulLore.add("");
        powerfulLore.add(ChatColor.GRAY + "Enchant tiers: " + ChatColor.LIGHT_PURPLE + "III - V");
        powerfulLore.add(ChatColor.GRAY + "Tier chances:");
        powerfulLore.add(ChatColor.GRAY + "  • Tier III: " + ChatColor.WHITE + "70%");
        powerfulLore.add(ChatColor.GRAY + "  • Tier IV: " + ChatColor.WHITE + "25%");
        powerfulLore.add(ChatColor.GRAY + "  • Tier V: " + ChatColor.WHITE + "5%");
        powerfulLore.add("");
        powerfulLore.add(ChatColor.YELLOW + "Click to select");
        powerfulMeta.setLore(powerfulLore);
        powerfulBook.setItemMeta(powerfulMeta);
        inv.setItem(15, powerfulBook);

        // Cancel button
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = barrier.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + "Cancel");
        barrier.setItemMeta(bMeta);
        inv.setItem(22, barrier);

        player.openInventory(inv);
    }

    /**
     * Opens the confirmation menu for the selected enchanting type.
     */
    private void openConfirmMenu(Player player, ItemStack tool, int cost, boolean isPowerful) {
        String title = isPowerful ? ChatColor.LIGHT_PURPLE + "Powerful Enchanting" : ChatColor.GREEN + "Basic Enchanting";
        Inventory inv = Bukkit.createInventory(new EnchantConfirmMenu(tool, cost, isPowerful), 27, title);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        ItemStack book = new ItemStack(isPowerful ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Confirm Enchant");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + cost + " crystals");
        lore.add("");
        if (isPowerful) {
            lore.add(ChatColor.LIGHT_PURPLE + "Powerful Enchanting");
            lore.add(ChatColor.GRAY + "Tiers: III - V");
        } else {
            lore.add(ChatColor.GREEN + "Basic Enchanting");
            lore.add(ChatColor.GRAY + "Tiers: I - III");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Randomly grants Mining Speed");
        lore.add(ChatColor.GRAY + "or Duplicate (10% chance for both)");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to purchase");
        meta.setLore(lore);
        book.setItemMeta(meta);
        inv.setItem(13, book);

        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = barrier.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + "Cancel");
        barrier.setItemMeta(bMeta);
        inv.setItem(22, barrier);

        player.openInventory(inv);
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Handle Selection Menu
        if (event.getInventory().getHolder() instanceof EnchantSelectionMenu menu) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            int slot = event.getSlot();
            if (slot == 11) {
                // Basic Enchanting selected
                openConfirmMenu(player, menu.tool, menu.baseCost, false);
            } else if (slot == 15) {
                // Powerful Enchanting selected
                int powerfulCost = menu.baseCost * 10;
                openConfirmMenu(player, menu.tool, powerfulCost, true);
            } else if (slot == 22) {
                player.closeInventory();
            }
            return;
        }

        // Handle Confirm Menu
        if (event.getInventory().getHolder() instanceof EnchantConfirmMenu menu) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if (event.getSlot() == 13) {
                if (!removeCrystals(player, menu.cost)) {
                    player.sendMessage(ChatColor.RED + "You need " + menu.cost + " crystals.");
                    return;
                }
                List<EnchantType> enchants = chooseEnchantments();
                applyEnchantments(menu.tool, enchants, menu.isPowerful);
                player.getInventory().setItemInMainHand(menu.tool);
                player.updateInventory();
                String typeMsg = menu.isPowerful ? "Powerful" : "Basic";
                player.sendMessage(ChatColor.GREEN + "Pickaxe enchanted with " + typeMsg + " enchants!");
                player.closeInventory();
            } else if (event.getSlot() == 22) {
                player.closeInventory();
            }
            return;
        }

        // Legacy support for old EnchantMenu (backward compatibility)
        if (event.getInventory().getHolder() instanceof EnchantMenu menu) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if (event.getSlot() == 13) {
                if (!removeCrystals(player, menu.cost)) {
                    player.sendMessage(ChatColor.RED + "You need " + menu.cost + " crystals.");
                    return;
                }
                List<EnchantType> enchants = chooseEnchantments();
                applyEnchantments(menu.tool, enchants, false);
                player.getInventory().setItemInMainHand(menu.tool);
                player.updateInventory();
                player.sendMessage(ChatColor.GREEN + "Pickaxe enchanted!");
                player.closeInventory();
            } else if (event.getSlot() == 22) {
                player.closeInventory();
            }
        }
    }

    private static class EnchantSelectionMenu implements InventoryHolder {
        private final ItemStack tool;
        private final int baseCost;

        EnchantSelectionMenu(ItemStack tool, int baseCost) {
            this.tool = tool;
            this.baseCost = baseCost;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class EnchantConfirmMenu implements InventoryHolder {
        private final ItemStack tool;
        private final int cost;
        private final boolean isPowerful;

        EnchantConfirmMenu(ItemStack tool, int cost, boolean isPowerful) {
            this.tool = tool;
            this.cost = cost;
            this.isPowerful = isPowerful;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class EnchantMenu implements InventoryHolder {
        private final ItemStack tool;
        private final int cost;

        EnchantMenu(ItemStack tool, int cost) {
            this.tool = tool;
            this.cost = cost;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private int getBaseCost(Material material) {
        return switch (material) {
            case WOODEN_PICKAXE -> 64;
            case STONE_PICKAXE -> 128;
            case IRON_PICKAXE -> 256;
            case GOLDEN_PICKAXE -> 512;
            case DIAMOND_PICKAXE -> 1024;
            case NETHERITE_PICKAXE -> 2048;
            default -> -1;
        };
    }

    private boolean removeCrystals(Player player, int amount) {
        return CrystalCurrency.removeCrystals(player, amount);
    }

    private List<EnchantType> chooseEnchantments() {
        List<EnchantType> result = new ArrayList<>();
        if (random.nextDouble() < 0.10) {
            result.add(EnchantType.MINING_SPEED);
            result.add(EnchantType.DUPLICATE);
        } else {
            if (random.nextBoolean()) {
                result.add(EnchantType.MINING_SPEED);
            } else {
                result.add(EnchantType.DUPLICATE);
            }
        }
        return result;
    }

    /**
     * Returns a random level for Basic enchanting (tiers 1-3).
     * 70% for tier 1, 25% for tier 2, 5% for tier 3.
     */
    private int randomBasicLevel() {
        double roll = random.nextDouble();
        if (roll < 0.70) return 1;
        if (roll < 0.95) return 2;
        return 3;
    }

    /**
     * Returns a random level for Powerful enchanting (tiers 3-5).
     * 70% for tier 3, 25% for tier 4, 5% for tier 5.
     */
    private int randomPowerfulLevel() {
        double roll = random.nextDouble();
        if (roll < 0.70) return 3;
        if (roll < 0.95) return 4;
        return 5;
    }

    private void applyEnchantments(ItemStack item, List<EnchantType> enchantments, boolean isPowerful) {
        for (EnchantType enchantment : enchantments) {
            int level = isPowerful ? randomPowerfulLevel() : randomBasicLevel();
            switch (enchantment) {
                case MINING_SPEED -> CustomTool.addMiningSpeed(plugin, item, level);
                case DUPLICATE -> CustomTool.addDuplicate(plugin, item, level);
            }
        }
    }

    private enum EnchantType {
        MINING_SPEED,
        DUPLICATE
    }
}

