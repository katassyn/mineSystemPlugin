package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.tool.ToolListener;

public final class MineSystemPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register listeners
        getServer().getPluginManager().registerEvents(new ToolListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
