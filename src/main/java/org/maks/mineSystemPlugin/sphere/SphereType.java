package org.maks.mineSystemPlugin.sphere;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Represents the overall type of a mining sphere.
 */
public enum SphereType {
    ORE("Ore", 45, "Ore Sphere"),
    TREASURE("Treasure", 11, "Treasure Sphere"),
    VEGETATION("Vegetation", 15, "Vegetation Sphere"),
    MOB("Mob", 17, "Mob Sphere"),
    BOSS("Boss", 1, "Boss Sphere"),
    SPECIAL_EVENT("SpecialEvent", 5, "Special Sphere"),
    CRYSTAL_DUST("CrystalDust", 5, "Crystal Dust Sphere");


    private static final Random RANDOM = new Random();

    private final String folderName;
    private final int weight;
    private final String displayName;

    SphereType(String folderName, int weight, String displayName) {
        this.folderName = folderName;
        this.weight = weight;
        this.displayName = displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
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
