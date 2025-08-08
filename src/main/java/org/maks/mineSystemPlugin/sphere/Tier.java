package org.maks.mineSystemPlugin.sphere;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Represents the rarity tier for ores inside a sphere.
 */
public enum Tier {
    TYPE_I(70),
    TYPE_II(20),
    TYPE_III(10);

    private static final Random RANDOM = new Random();

    private final int weight;

    Tier(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public static Tier random() {
        List<Tier> tiers = Arrays.asList(values());
        int total = tiers.stream().mapToInt(Tier::getWeight).sum();
        int r = RANDOM.nextInt(total);
        int current = 0;
        for (Tier tier : tiers) {
            current += tier.weight;
            if (r < current) {
                return tier;
            }
        }
        return TYPE_I;
    }
}
