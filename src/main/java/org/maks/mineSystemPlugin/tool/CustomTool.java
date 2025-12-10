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
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

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

        // Only treat the item as a plugin tool if it already carries the
        // formatted durability lore line. Vanilla tools should remain
        // untouched so they cannot bypass sphere restrictions.
        List<String> lore = meta.getLore();
        boolean hasDurabilityLore = lore != null && lore.stream()
                .map(ChatColor::stripColor)
                .filter(l -> l != null)
                .anyMatch(l -> l.trim().startsWith("Durability:"));

        if (!hasDurabilityLore) {
            return; // not a custom tool managed by this plugin
        }

        // capture CanDestroy before modifying meta so we can restore it later
        var canDestroy = meta.getCanDestroy();

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");
        NamespacedKey markerKey = new NamespacedKey(plugin, "custom_tool");
        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        Integer cur = pdc.get(curKey, PersistentDataType.INTEGER);

        // mark item as plugin tool if not already
        if (!pdc.has(markerKey, PersistentDataType.BYTE)) {
            pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        }


        // initialise missing durability data
        if (max == null || cur == null) {
            ToolMaterial material = ToolMaterial.fromMaterial(item.getType());
            if (material == null) {
                item.setItemMeta(meta);
                return;
            }
            int value = material.getMaxDurability();
            pdc.set(maxKey, PersistentDataType.INTEGER, value);
            pdc.set(curKey, PersistentDataType.INTEGER, value);

            if (lore == null) lore = new ArrayList<>();
            int index = findDurabilityIndex(lore);
            String formatted = formatDurability(value, value);
            if (index == -1) {
                lore.add(formatted);
            } else {
                lore.set(index, formatted);
            }
            meta.setLore(lore);
        }

        // preserve CanDestroy values when reapplying meta
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
     * Adds a custom Duplicate enchant. Levels correspond to:
     * Level 1: 5%, Level 2: 10%, Level 3: 15%, Level 4: 22%, Level 5: 30%
     * chance to drop an extra item. Lore is refreshed to follow the enchanted pickaxe
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

        var canDestroy = meta.getCanDestroy();

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
        if (canDestroy != null && !canDestroy.isEmpty()) {
            meta.setCanDestroy(new HashSet<>(canDestroy));
        }

        item.setItemMeta(meta);
        return cur <= 0;
    }

    /**
     * Returns the current and maximum durability stored on the item.
     * Primarily used for debugging purposes.
     *
     * @return array of [current, max] durability or {@code null} if not present
     */
    public static int[] getDurability(ItemStack item, Plugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey maxKey = new NamespacedKey(plugin, "max_durability");
        NamespacedKey curKey = new NamespacedKey(plugin, "durability");

        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        Integer cur = pdc.get(curKey, PersistentDataType.INTEGER);
        if (max == null || cur == null) return null;
        return new int[] { cur, max };
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
