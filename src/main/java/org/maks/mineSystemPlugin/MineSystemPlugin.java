package org.maks.mineSystemPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.managers.PickaxeManager;
import org.maks.mineSystemPlugin.managers.SphereManager;
import org.maks.mineSystemPlugin.managers.StaminaManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MineSystemPlugin extends JavaPlugin {

    private Connection connection;
    private StaminaManager staminaManager;
    private SphereManager sphereManager;
    private PickaxeManager pickaxeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        try {
            String host = config.getString("mysql.host");
            int port = config.getInt("mysql.port");
            String database = config.getString("mysql.database");
            String username = config.getString("mysql.username");
            String password = config.getString("mysql.password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";

            connection = DriverManager.getConnection(url, username, password);

            staminaManager = new StaminaManager(this);
            sphereManager = new SphereManager(this);
            pickaxeManager = new PickaxeManager(this);
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (staminaManager != null) {
            staminaManager.saveAll();
        }
        if (sphereManager != null) {
            sphereManager.saveAll();
        }
        if (pickaxeManager != null) {
            pickaxeManager.saveAll();
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                getLogger().severe("Failed to close MySQL connection: " + e.getMessage());
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public StaminaManager getStaminaManager() {
        return staminaManager;
    }

    public SphereManager getSphereManager() {
        return sphereManager;
    }

    public PickaxeManager getPickaxeManager() {
        return pickaxeManager;
    }
}
