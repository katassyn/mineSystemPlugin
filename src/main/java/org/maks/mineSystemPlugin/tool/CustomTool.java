package org.maks.mineSystemPlugin.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;

/**
 * Factory and utility methods for custom mining tools.
 */
public final class CustomTool {

    private CustomTool() {
    }

    /**
     * Creates a new pickaxe of the given material and initial durability.
     * The second lore line is always formatted as "Durability: X/Y".
     *
     * @param plugin      plugin instance for namespaced keys
     * @param material    tool material type
     * @param name        display name of the item
     * @param description first lore line description
     * @param canDestroy  set of blocks the tool can destroy
     * @return item stack representing the tool
     */
    public static ItemStack createTool(Plugin plugin, ToolMaterial material, String name, String description,
            Set<Material> canDestroy) {
        ItemStack stack = new ItemStack(material.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.displayName(Component.text(name));

        // base lore with durability line
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', description));
        lore.add(formatDurability(material.getMaxDurability(), material.getMaxDurability()));
        meta.setLore(lore);

        // persistence
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");
        pdc.set(maxKey, PersistentDataType.INTEGER, material.getMaxDurability());
        pdc.set(curKey, PersistentDataType.INTEGER, material.getMaxDurability());

        // allowed blocks restriction
        if (canDestroy != null && !canDestroy.isEmpty()) {
            meta.setCanDestroy(new HashSet<>(canDestroy));
        }

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Adds the vanilla DIG_SPEED enchantment using a custom name "Mining Speed".
     * A lore line describing the enchantment is inserted after the durability line
     * (always at index 2). New enchant descriptions are added from the top.
     */
    public static void addMiningSpeed(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.addEnchant(Enchantment.DIG_SPEED, level, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        insertLoreLine(meta, ChatColor.AQUA + "Mining Speed " + roman(level));
        item.setItemMeta(meta);
    }

    /**
     * Adds a custom Duplicate enchant. Levels correspond to 3%, 4% and 5% chance
     * to drop an extra item. Lore line is inserted from the top like other
     * enchantments.
     */
    public static void addDuplicate(Plugin plugin, ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey dupKey = new NamespacedKey(plugin, "duplicate");
        pdc.set(dupKey, PersistentDataType.INTEGER, level);

        // give item a visual enchant glint
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.DURABILITY, 1, true);

        insertLoreLine(meta, ChatColor.LIGHT_PURPLE + "Duplicate " + roman(level));
        item.setItemMeta(meta);
    }

    /**
     * Handles durability reduction and lore update after a block is mined.
     *
     * @return true if the item is broken after the hit
     */
    public static boolean damage(ItemStack item, Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");

        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        Integer cur = pdc.get(curKey, PersistentDataType.INTEGER);
        if (max == null || cur == null) return false;

        cur = Math.max(0, cur - 1);
        pdc.set(curKey, PersistentDataType.INTEGER, cur);

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        if (lore.size() < 2) {
            lore.add(" ");
        }
        lore.set(1, formatDurability(cur, max));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return cur <= 0;
    }

    public static int getDuplicateLevel(ItemStack item, Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey dupKey = new NamespacedKey(plugin, "duplicate");
        Integer level = pdc.get(dupKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    private static void insertLoreLine(ItemMeta meta, String line) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
            lore.add(" ");
            lore.add(" ");
        } else {
            while (lore.size() < 2) {
                lore.add(" ");
            }
        }
        lore.add(2, line);
        meta.setLore(lore);
    }

    private static String formatDurability(int cur, int max) {
        return ChatColor.GRAY + "Durability: " + cur + "/" + max;
    }

    private static String roman(int number) {
        return switch (number) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "I";
        };
    }
}
