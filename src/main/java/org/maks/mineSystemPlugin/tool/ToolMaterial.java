package org.maks.mineSystemPlugin.tool;

import org.bukkit.Material;

/**
 * Enumeration of tool materials with custom maximum durability values.
 */
public enum ToolMaterial {
    WOODEN(Material.WOODEN_PICKAXE, 2500),
    STONE(Material.STONE_PICKAXE, 5000),
    IRON(Material.IRON_PICKAXE, 20000),
    GOLD(Material.GOLDEN_PICKAXE, 10000),
    DIAMOND(Material.DIAMOND_PICKAXE, 50000),
    NETHERITE(Material.NETHERITE_PICKAXE, 100000);

    private final Material material;
    private final int maxDurability;

    ToolMaterial(Material material, int maxDurability) {
        this.material = material;
        this.maxDurability = maxDurability;
    }

    public Material getMaterial() {
        return material;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    /**
     * Resolves the enum constant for the given Bukkit material.
     *
     * @param material Bukkit material of the pickaxe
     * @return matching ToolMaterial or {@code null} if unsupported
     */
    public static ToolMaterial fromMaterial(Material material) {
        for (ToolMaterial value : values()) {
            if (value.material == material) {
                return value;
            }
        }
        return null;
    }
}
