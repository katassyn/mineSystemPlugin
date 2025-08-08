package org.maks.mineSystemPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.sphere.SphereListener;
import org.maks.mineSystemPlugin.sphere.SphereManager;

public final class MineSystemPlugin extends JavaPlugin {

    private SphereManager sphereManager;

    @Override
    public void onEnable() {
        this.sphereManager = new SphereManager(this);
        Bukkit.getPluginManager().registerEvents(new SphereListener(sphereManager), this);
        getDataFolder().mkdirs();
    }

    @Override
    public void onDisable() {
        if (sphereManager != null) {
            sphereManager.removeAll();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("spawnsphere")) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        sphereManager.createSphere((Player) sender);
        return true;
    }
}
