package org.maks.mineSystemPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.command.LootCommand;
import org.maks.mineSystemPlugin.command.SphereCommand;
import org.maks.mineSystemPlugin.sphere.SphereManager;
import org.maks.mineSystemPlugin.sphere.SphereListener;
import org.maks.mineSystemPlugin.storage.MySqlStorage;
import org.maks.mineSystemPlugin.SpecialBlockListener;
import org.maks.mineSystemPlugin.CrystalEnchantCommand;
import org.maks.mineSystemPlugin.RepairCommand;
import org.maks.mineSystemPlugin.LootManager;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Main plugin entry point.
 */
public final class MineSystemPlugin extends JavaPlugin {
    private MySqlStorage storage;
    private LootManager lootManager;
    private SphereManager sphereManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // initialize storage and loot configuration
        try {
            String host = getConfig().getString("mysql.host");
            int port = getConfig().getInt("mysql.port");
            String database = getConfig().getString("mysql.database");
            String user = getConfig().getString("mysql.username");
            String pass = getConfig().getString("mysql.password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            storage = new MySqlStorage(url, user, pass);
            lootManager = new LootManager();
            lootManager.setProbabilities(storage.loadItems());
            getCommand("loot").setExecutor(new LootCommand(storage, lootManager));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to connect to MySQL", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // register commands
        getCommand("repair").setExecutor(new RepairCommand(this));
        getCommand("crystalenchant").setExecutor(new CrystalEnchantCommand());
        getCommand("sphere").setExecutor(new SphereCommand());
        getCommand("spawnsphere").setExecutor(this);

        // listeners and managers
        sphereManager = new SphereManager(this);
        getServer().getPluginManager().registerEvents(new SpecialBlockListener(), this);
        getServer().getPluginManager().registerEvents(new SphereListener(sphereManager), this);
    }

    @Override
    public void onDisable() {
        if (sphereManager != null) {
            sphereManager.removeAll();
        }
        if (storage != null) {
            storage.close();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("spawnsphere")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command");
                return true;
            }
            if (sphereManager != null) {
                sphereManager.createSphere(player);
            }
            return true;
        }
        return false;
    }

    public SphereManager getSphereManager() {
        return sphereManager;
    }
}
