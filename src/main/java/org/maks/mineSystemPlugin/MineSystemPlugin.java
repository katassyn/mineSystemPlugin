package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point. Registers the repair command and loads the
 * configuration containing the base repair cost.
 */
public final class MineSystemPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // ensure the config file exists
        saveDefaultConfig();

        // register the repair command
        getCommand("repair").setExecutor(new RepairCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
