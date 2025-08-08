package org.maks.mineSystemPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Command that repairs the item held in the player's main hand. The cost is
 * calculated based on the material tier and enchantments of the item.
 */
public class RepairCommand implements CommandExecutor {
    private final MineSystemPlugin plugin;

    public RepairCommand(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("Hold the item you wish to repair in your main hand.");
            return true;
        }

        int cost = calculateCost(item);
        if (!hasCrystals(player, cost)) {
            player.sendMessage("Not enough Crystals. Required: " + cost);
            return true;
        }

        removeCrystals(player, cost);
        repairItem(item);
        player.sendMessage("Item repaired for " + cost + " Crystals.");
        return true;
    }

    private int calculateCost(ItemStack item) {
        int base = plugin.getConfig().getInt("base-cost", 32);
        int tierIndex = materialTier(item.getType());
        int materialCost = base * (1 << tierIndex);

        Map<org.bukkit.enchantments.Enchantment, Integer> enchants = item.getEnchantments();
        int enchantCount = enchants.size();
        int multiplier = switch (enchantCount) {
            case 0 -> 1;
            case 1 -> 2;
            default -> 3;
        };
        int levelSum = enchants.values().stream().mapToInt(Integer::intValue).sum();
        if (levelSum < 1) {
            levelSum = 1;
        }

        return materialCost * multiplier * levelSum;
    }

    private int materialTier(Material material) {
        String name = material.toString();
        if (name.startsWith("WOODEN_")) return 0;
        if (name.startsWith("STONE_")) return 1;
        if (name.startsWith("IRON_")) return 2;
        if (name.startsWith("GOLDEN_")) return 3;
        if (name.startsWith("DIAMOND_")) return 4;
        if (name.startsWith("NETHERITE_")) return 5;
        return 0;
    }

    private boolean hasCrystals(Player player, int amount) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == Material.PRISMARINE_CRYSTALS) {
                total += stack.getAmount();
                if (total >= amount) {
                    return true;
                }
            }
        }
        return total >= amount;
    }

    private void removeCrystals(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == Material.PRISMARINE_CRYSTALS) {
                int toRemove = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - toRemove);
                remaining -= toRemove;
                if (stack.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setContents(contents);
    }

    private void repairItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.removeIf(line -> line.startsWith("Durability:"));
            int max = item.getType().getMaxDurability();
            lore.add("Durability: " + max + "/" + max);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }
}

