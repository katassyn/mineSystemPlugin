package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class MineSystemPlugin extends JavaPlugin {
    private SpecialBlockListener listener;

    @Override
    public void onEnable() {
        listener = new SpecialBlockListener();
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
