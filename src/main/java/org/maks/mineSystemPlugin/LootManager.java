package org.maks.mineSystemPlugin;

import java.util.*;

/**
 * Handles randomisation logic for loot rolls.
 */
public class LootManager {
    private final Random random = new Random();
    private Map<String, Double> probabilities = new HashMap<>();

    public void setProbabilities(Map<String, Double> probabilities) {
        this.probabilities = probabilities;
    }

    /**
     * Rolls number of items using normal distribution with mean 13.
     * The value is clamped to range 1-27.
     */
    public int rollItemCount() {
        int count = (int) Math.round(13 + random.nextGaussian() * 4);
        if (count < 1) count = 1;
        if (count > 27) count = 27;
        return count;
    }

    /**
     * Selects an item from configured probabilities.
     */
    public String rollItem() {
        if (probabilities.isEmpty()) {
            return null;
        }
        double total = probabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
            cumulative += entry.getValue();
            if (r <= cumulative) {
                return entry.getKey();
            }
        }
        return probabilities.keySet().iterator().next();
    }
}
