package org.maks.mineSystemPlugin.sphere;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Represents the overall type of a mining sphere.
 */
public enum SphereType {
    ORE("Ore", 45),
    TREASURE("Treasure", 11),
    VEGETATION("Vegetation", 15),
    MOB("Mob", 15),
    BOSS("Boss", 3),
    SPECIAL_EVENT("SpecialEvent", 5),
    CRYSTAL_DUST("CrystalDust", 5);

    private static final Random RANDOM = new Random();

    private final String folderName;
    private final int weight;

    SphereType(String folderName, int weight) {
        this.folderName = folderName;
        this.weight = weight;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Chooses a random sphere type using the configured weights.
     */
    public static SphereType random() {
        return random(Arrays.asList(values()));
    }

    /**
     * Chooses a random sphere type from the provided list, normalizing weights
     * so that excluded types do not skew the distribution.
     */
    public static SphereType random(List<SphereType> types) {
        int total = types.stream().mapToInt(SphereType::getWeight).sum();
        int r = RANDOM.nextInt(total);
        int current = 0;
        for (SphereType type : types) {
            current += type.weight;
            if (r < current) {
                return type;
            }
        }
        return types.get(0); // Fallback
    }
}
