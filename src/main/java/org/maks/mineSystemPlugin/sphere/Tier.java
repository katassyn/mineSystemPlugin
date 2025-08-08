package org.maks.mineSystemPlugin.sphere;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Represents the rarity tier for ores inside a sphere.
 */
public enum Tier {
    TYPE_I(70),
    TYPE_II(25),
    TYPE_III(5);

    private static final Random RANDOM = new Random();

    private final int weight;

    Tier(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Returns a random tier. In premium spheres the distribution is reversed
     * so that {@code TYPE_I} is rarest and {@code TYPE_III} is most common.
     *
     * @param premium whether to use the premium distribution
     * @return random tier
     */
    public static Tier random(boolean premium) {
        List<Tier> tiers = Arrays.asList(values());
        int[] weights = premium ? new int[]{5, 25, 70} : new int[]{70, 25, 5};

        int total = Arrays.stream(weights).sum();
        int r = RANDOM.nextInt(total);
        int current = 0;
        for (int i = 0; i < tiers.size(); i++) {
            current += weights[i];
            if (r < current) {
                return tiers.get(i);
            }
        }
        return TYPE_I;
    }

    /**
     * Convenience method using the regular distribution.
     */
    public static Tier random() {
        return random(false);
    }
}
