package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;

import org.maks.mineSystemPlugin.command.SphereCommand;

public final class MineSystemPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("sphere").setExecutor(new SphereCommand());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
