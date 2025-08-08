package org.maks.mineSystemPlugin;

public class StaminaManager {
    private int currentStamina;
    private int maxStamina;
    private long firstUsageTimestamp; // millis
    private static final int ENTRY_COST = 10;
    private static final long RESET_INTERVAL_MILLIS = 12L * 60 * 60 * 1000; // 12 hours

    public StaminaManager(int maxStamina) {
        this.maxStamina = maxStamina;
        this.currentStamina = maxStamina;
        this.firstUsageTimestamp = 0; // 0 means not used yet after last reset
    }

    public int getCurrentStamina() {
        return currentStamina;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(int newMax) {
        this.maxStamina = newMax;
        if (currentStamina > maxStamina) {
            currentStamina = maxStamina;
        }
    }

    /**
     * Attempts to enter a sphere. Costs 10 stamina.
     * Returns true if the player has enough stamina, false otherwise.
     */
    public boolean enterSphere() {
        resetIfNeeded();
        if (currentStamina < ENTRY_COST) {
            return false;
        }
        if (firstUsageTimestamp == 0) {
            firstUsageTimestamp = System.currentTimeMillis();
        }
        currentStamina -= ENTRY_COST;
        return true;
    }

    private void resetIfNeeded() {
        if (firstUsageTimestamp != 0 &&
                System.currentTimeMillis() - firstUsageTimestamp >= RESET_INTERVAL_MILLIS) {
            currentStamina = maxStamina;
            firstUsageTimestamp = 0;
        }
    }
}
