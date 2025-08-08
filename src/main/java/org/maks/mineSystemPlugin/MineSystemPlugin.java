package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.listener.OreBreakListener;

public class MineSystemPlugin extends JavaPlugin {

    private int globalOreCount = 0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new OreBreakListener(this), this);
    }

    public int incrementOreCount() {
        return ++globalOreCount;
    }
}

