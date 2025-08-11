package org.maks.mineSystemPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;

/**
 * Command that repairs the item held in the player's main hand. The cost is
 * calculated based on the material tier and enchantments of the item.
 */
public class RepairCommand implements CommandExecutor, Listener {
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
        if (item == null || item.getType().isAir() || !item.getType().name().endsWith("_PICKAXE")) {
            player.sendMessage("Hold the pickaxe you wish to repair in your main hand.");
            return true;
        }

        int cost = calculateCost(item);
        if (cost <= 0) {
            player.sendMessage("That pickaxe doesn't need repairing.");
            return true;
        }

        openMenu(player, item, cost);
        return true;
    }

    private void openMenu(Player player, ItemStack tool, int cost) {
        Inventory inv = Bukkit.createInventory(new RepairMenu(tool, cost), 27, ChatColor.DARK_GREEN + "Repair Pickaxe");

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta aMeta = anvil.getItemMeta();
        aMeta.setDisplayName(ChatColor.GREEN + "Repair");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + cost + " crystals");
        aMeta.setLore(lore);
        anvil.setItemMeta(aMeta);
        inv.setItem(13, anvil);

        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = barrier.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + "Cancel");
        barrier.setItemMeta(bMeta);
        inv.setItem(22, barrier);

        player.openInventory(inv);
    }

    private int calculateCost(ItemStack item) {
        int base = 32;
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

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int missing = damageable.getDamage();
            int max = item.getType().getMaxDurability();
            if (missing <= 0 || max <= 0) return 0;
            double fraction = missing / (double) max;
            return (int) Math.ceil(materialCost * multiplier * levelSum * fraction);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");
        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        Integer cur = pdc.get(curKey, PersistentDataType.INTEGER);
        if (max != null && cur != null && max > 0) {
            int missing = max - cur;
            if (missing <= 0) return 0;
            double fraction = missing / (double) max;
            return (int) Math.ceil(materialCost * multiplier * levelSum * fraction);
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
        if (meta == null) {
            return;
        }

        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");
        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        if (max != null) {
            pdc.set(curKey, PersistentDataType.INTEGER, max);
        }

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Durability:"));
        int maxDurability = max != null ? max : item.getType().getMaxDurability();
        lore.add(ChatColor.GRAY + "Durability: " + maxDurability + "/" + maxDurability);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RepairMenu menu)) {
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
            if (!hasCrystals(player, menu.cost)) {
                player.sendMessage(ChatColor.RED + "Not enough Crystals. Required: " + menu.cost);
                return;
            }
            removeCrystals(player, menu.cost);
            repairItem(menu.tool);
            player.sendMessage(ChatColor.GREEN + "Item repaired for " + menu.cost + " Crystals.");
            player.closeInventory();
        } else if (event.getSlot() == 22) {
            player.closeInventory();
        }
    }

    private static class RepairMenu implements InventoryHolder {
        private final ItemStack tool;
        private final int cost;

        RepairMenu(ItemStack tool, int cost) {
            this.tool = tool;
            this.cost = cost;
        }

        @Override
        public Inventory getInventory() {
            return null; // not used
        }
    }
}

