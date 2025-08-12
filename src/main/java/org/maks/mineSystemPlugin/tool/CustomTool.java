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
        NamespacedKey markerKey = new NamespacedKey(plugin, "custom_tool");
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);

        // allowed blocks restriction
        if (canDestroy != null && !canDestroy.isEmpty()) {
            meta.setCanDestroy(new HashSet<>(canDestroy));
        }

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Ensures the given item has durability metadata and lore initialised. This
     * allows tools obtained from configuration to participate in the custom
     * durability system even if they lacked persistent data.
     */
    public static void ensureDurability(ItemStack item, Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");
        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        Integer cur = pdc.get(curKey, PersistentDataType.INTEGER);
        if (max != null && cur != null) {
            return; // already initialised
        }

        ToolMaterial material = ToolMaterial.fromMaterial(item.getType());
        if (material == null) return;
        int value = material.getMaxDurability();
        pdc.set(maxKey, PersistentDataType.INTEGER, value);
        pdc.set(curKey, PersistentDataType.INTEGER, value);

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        int index = findDurabilityIndex(lore);
        String formatted = formatDurability(value, value);
        if (index == -1) {
            lore.add(formatted);
        } else {
            lore.set(index, formatted);
        }
        meta.setLore(lore);

        // preserve CanDestroy values when reapplying meta
        var canDestroy = meta.getCanDestroy();
        if (canDestroy != null && !canDestroy.isEmpty()) {
            meta.setCanDestroy(new HashSet<>(canDestroy));
        }

        item.setItemMeta(meta);
    }

    /**
     * Adds the vanilla DIG_SPEED enchantment using a custom name and refreshes the
     * lore so it matches the pattern defined in {@code itemy.md}.
     */
    public static void addMiningSpeed(Plugin plugin, ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.addEnchant(Enchantment.DIG_SPEED, level, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        updateEnchantLore(item, plugin);
    }

    /**
     * Adds a custom Duplicate enchant. Levels correspond to 3%, 4% and 5% chance
     * to drop an extra item. Lore is refreshed to follow the enchanted pickaxe
     * example from {@code itemy.md}.
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
        item.setItemMeta(meta);

        updateEnchantLore(item, plugin);
    }

    /**
     * Rebuilds the lore so the first lines list enchantment levels followed by a
     * blank separator line and the original description and durability.
     */
    private static void updateEnchantLore(ItemStack item, Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int speed = meta.getEnchantLevel(Enchantment.DIG_SPEED);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey dupKey = new NamespacedKey(plugin, "duplicate");
        Integer dup = pdc.get(dupKey, PersistentDataType.INTEGER);

        List<String> existing = meta.getLore();
        List<String> lore = new ArrayList<>();
        if (speed > 0) {
            lore.add(ChatColor.GRAY + "Mining speed: " + ChatColor.YELLOW + speed + ChatColor.GRAY + " ⛏");
        }
        if (dup != null && dup > 0) {
            lore.add(ChatColor.GRAY + "Duplicate: " + ChatColor.YELLOW + dup + ChatColor.GRAY + " ☘");
        }
        if (!lore.isEmpty()) {
            lore.add("");
        }

        if (existing != null) {
            for (String line : existing) {
                String stripped = ChatColor.stripColor(line);
                if (stripped == null) continue;
                if (stripped.startsWith("Mining speed:") || stripped.startsWith("Duplicate:")) {
                    continue;
                }
                if (stripped.isEmpty()) {
                    continue;
                }
                lore.add(line);
            }
        }

        meta.setLore(lore);
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
        int durabilityIndex = findDurabilityIndex(lore);
        String formatted = formatDurability(cur, max);
        if (durabilityIndex == -1) {
            lore.add(formatted);
        } else {
            lore.set(durabilityIndex, formatted);
        }
        meta.setLore(lore);

        // ensure CanDestroy is preserved when setting meta
        var canDestroy = meta.getCanDestroy();
        if (canDestroy != null && !canDestroy.isEmpty()) {
            meta.setCanDestroy(new HashSet<>(canDestroy));
        }

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

    /**
     * Determines the tool level based on its material. The lowest tier
     * (wooden) is level 1 while the highest (netherite) is level 6. If the
     * item does not match any known pickaxe material the level is 0.
     */
    public static int getToolLevel(ItemStack item) {
        Material type = item.getType();
        int level = 1;
        for (ToolMaterial material : ToolMaterial.values()) {
            if (material.getMaterial() == type) {
                return level;
            }
            level++;
        }
        return 0;
    }

    private static String formatDurability(int cur, int max) {
        return ChatColor.GRAY + "Durability: " + cur + "/" + max;
    }

    private static int findDurabilityIndex(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String stripped = ChatColor.stripColor(lore.get(i));
            if (stripped != null && stripped.startsWith("Durability:")) {
                return i;
            }
        }
        return -1;
    }

}
