package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class which sets up the {@link StaminaManager}.
 */
public final class MineSystemPlugin extends JavaPlugin {

    private StaminaManager staminaManager;

    @Override
    public void onEnable() {
        // Initialize stamina with a default max of 100. This value may grow with quests.
        staminaManager = new StaminaManager(100);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
