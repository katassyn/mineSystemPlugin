package org.maks.mineSystemPlugin.stamina;

import java.time.Instant;

/**
 * Simple POJO holding stamina information for a player.
 */
public class PlayerStamina {
    private int stamina;
    private int maxStamina;
    private Instant firstUsage;

    public PlayerStamina(int maxStamina) {
        // NIE ustawiamy staminy na 0 - będzie ustawiona przez StaminaManager
        // na podstawie danych z bazy lub na max dla nowych graczy
        this.stamina = -1; // -1 oznacza "nie zainicjalizowana"
        this.maxStamina = maxStamina;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(int maxStamina) {
        this.maxStamina = maxStamina;
    }

    public Instant getFirstUsage() {
        return firstUsage;
    }

    public void setFirstUsage(Instant firstUsage) {
        this.firstUsage = firstUsage;
    }

    /**
     * Checks if stamina has been initialized from database or set to default
     */
    public boolean isInitialized() {
        return stamina >= 0;
    }
}
