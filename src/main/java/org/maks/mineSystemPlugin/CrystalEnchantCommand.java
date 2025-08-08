package org.maks.mineSystemPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CrystalEnchantCommand implements CommandExecutor {

    private static final Material CURRENCY = Material.PRISMARINE_CRYSTALS;
    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
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

        if (!removeCrystals(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " crystals.");
            return true;
        }

        List<Enchantment> enchants = chooseEnchantments();
        applyEnchantments(item, enchants);

        player.sendMessage(ChatColor.GREEN + "Pickaxe enchanted!");
        return true;
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
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != CURRENCY) continue;
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() == 0) {
                contents[i] = null;
            }
            if (remaining <= 0) break;
        }
        player.getInventory().setContents(contents);
        return remaining <= 0;
    }

    private List<Enchantment> chooseEnchantments() {
        List<Enchantment> result = new ArrayList<>();
        if (random.nextDouble() < 0.10) {
            result.add(Enchantment.DIG_SPEED);
            result.add(Enchantment.LUCK);
        } else {
            if (random.nextBoolean()) {
                result.add(Enchantment.DIG_SPEED);
            } else {
                result.add(Enchantment.LUCK);
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

    private void applyEnchantments(ItemStack item, List<Enchantment> enchantments) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        for (Enchantment enchantment : enchantments) {
            int level = randomLevel();
            meta.addEnchant(enchantment, level, true);
            lore.add(ChatColor.GRAY + hiddenName(enchantment) + " " + level);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String hiddenName(Enchantment enchantment) {
        if (enchantment.equals(Enchantment.DIG_SPEED)) {
            return "Efficiency";
        }
        if (enchantment.equals(Enchantment.LUCK)) {
            return "Luck";
        }
        return enchantment.getKey().getKey();
    }
}

