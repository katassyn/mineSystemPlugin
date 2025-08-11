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

        int cost = getCost(item.getType());
        if (cost <= 0) {
            player.sendMessage(ChatColor.RED + "You must hold a valid pickaxe.");
            return true;
        }

        openMenu(player, item, cost);
        return true;
    }

    private void openMenu(Player player, ItemStack tool, int cost) {
        Inventory inv = Bukkit.createInventory(new EnchantMenu(tool, cost), 27, ChatColor.DARK_AQUA + "Crystal Enchants");

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Purchase Enchant");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + cost + " crystals");
        lore.add(ChatColor.GRAY + "Randomly grants Mining Speed");
        lore.add(ChatColor.GRAY + "or Duplicate (10% both)");
        lore.add(ChatColor.YELLOW + "Click to buy");
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
        if (!(event.getInventory().getHolder() instanceof EnchantMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

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
            applyEnchantments(menu.tool, enchants);
            player.getInventory().setItemInMainHand(menu.tool);
            player.updateInventory(); // opcjonalnie, aby odświeżyć ekwipunek
            player.sendMessage(ChatColor.GREEN + "Pickaxe enchanted!");
            player.closeInventory();
        } else if (event.getSlot() == 22) {
            player.closeInventory();
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
            return null; // not used
        }
    }

    private int getCost(Material material) {
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

    private int randomLevel() {
        double roll = random.nextDouble();
        if (roll < 0.70) return 1;
        if (roll < 0.95) return 2;
        return 3;
    }

    private void applyEnchantments(ItemStack item, List<EnchantType> enchantments) {
        for (EnchantType enchantment : enchantments) {
            int level = randomLevel();
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

