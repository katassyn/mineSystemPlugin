package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class MineSystemPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("crystalenchant").setExecutor(new CrystalEnchantCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
