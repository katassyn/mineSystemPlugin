package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.command.JoinSphereCommand;
import org.maks.mineSystemPlugin.mob.MobSpawner;
import org.maks.mineSystemPlugin.sphere.SphereManager;
import org.maks.mineSystemPlugin.stamina.StaminaManager;

import java.time.Duration;

public final class MineSystemPlugin extends JavaPlugin {

    private StaminaManager staminaManager;
    private SphereManager sphereManager;
    private MobSpawner mobSpawner;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int maxStamina = getConfig().getInt("maxStamina", 100);
        int sphereLimit = getConfig().getInt("sphereLimit", 20);

        this.staminaManager = new StaminaManager(this, maxStamina, Duration.ofHours(12));
        this.mobSpawner = new MobSpawner(this);
        this.sphereManager = new SphereManager(this, sphereLimit, mobSpawner);

        if (getCommand("sphere") != null) {
            getCommand("sphere").setExecutor(new JoinSphereCommand(this));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public StaminaManager getStaminaManager() {
        return staminaManager;
    }

    public SphereManager getSphereManager() {
        return sphereManager;
    }

    public MobSpawner getMobSpawner() {
        return mobSpawner;
    }
}
