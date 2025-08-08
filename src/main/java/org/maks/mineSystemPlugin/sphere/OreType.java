package org.maks.mineSystemPlugin.sphere;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Possible ore types generated in spheres.
 */
public enum OreType {
    COAL(Material.COAL_ORE, 25),
    IRON(Material.IRON_ORE, 20),
    LAPIS(Material.LAPIS_ORE, 15),
    REDSTONE(Material.REDSTONE_ORE, 12),
    GOLD(Material.GOLD_ORE, 10),
    EMERALD(Material.EMERALD_ORE, 10),
    DIAMOND(Material.DIAMOND_ORE, 8);

    private static final Random RANDOM = new Random();

    private final Material material;
    private final int weight;

    OreType(Material material, int weight) {
        this.material = material;
        this.weight = weight;
    }

    public Material getMaterial() {
        return material;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Randomly selects an ore using the configured weights.
     */
    public static OreType random() {
        List<OreType> types = Arrays.asList(values());
        int total = types.stream().mapToInt(OreType::getWeight).sum();
        int r = RANDOM.nextInt(total);
        int current = 0;
        for (OreType type : types) {
            current += type.weight;
            if (r < current) {
                return type;
            }
        }
        return COAL; // Fallback
    }
}
