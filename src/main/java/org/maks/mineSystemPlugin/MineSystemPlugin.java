package org.maks.mineSystemPlugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.events.OreMinedEvent;
import org.maks.mineSystemPlugin.events.SphereCompleteEvent;

import java.util.Map;

public final class MineSystemPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Reports that a player has mined a certain amount of ore. The event is dispatched so
     * quest systems and other plugins can listen for mining progress.
     */
    public void reportOreMined(Player player, Material oreType, int amount, int pickaxeLevel) {
        OreMinedEvent event = new OreMinedEvent(player, oreType, amount, pickaxeLevel);
        getServer().getPluginManager().callEvent(event);
    }

    /**
     * Reports that a player has completed a mining sphere.
     *
     * @param player      player who finished the sphere
     * @param type        type of sphere that was completed
     * @param statistics  statistics describing the completed sphere
     */
    public void reportSphereComplete(Player player, String type, Map<String, Integer> statistics) {
        SphereCompleteEvent event = new SphereCompleteEvent(player, type, statistics);
        getServer().getPluginManager().callEvent(event);
    }
}
