package org.maks.mineSystemPlugin.stamina;

import java.time.Instant;

/**
 * Simple POJO holding stamina information for a player.
 */
public class PlayerStamina {
    private int stamina;
    private Instant firstUsage;

    public PlayerStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public Instant getFirstUsage() {
        return firstUsage;
    }

    public void setFirstUsage(Instant firstUsage) {
        this.firstUsage = firstUsage;
    }
}
