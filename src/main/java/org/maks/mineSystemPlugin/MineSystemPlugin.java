package org.maks.mineSystemPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.sphere.SphereListener;
import org.maks.mineSystemPlugin.sphere.SphereManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.database.dao.PickaxesDao;
import org.maks.mineSystemPlugin.database.dao.PlayersDao;
import org.maks.mineSystemPlugin.database.dao.QuestsDao;
import org.maks.mineSystemPlugin.database.dao.SpheresDao;
import org.maks.mineSystemPlugin.managers.PickaxeManager;
import org.maks.mineSystemPlugin.managers.SphereManager;
import org.maks.mineSystemPlugin.managers.StaminaManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Main plugin class which sets up the {@link StaminaManager}.
 */
public final class MineSystemPlugin extends JavaPlugin {
    private DatabaseManager database;
    private PlayersDao playersDao;
    private PickaxesDao pickaxesDao;
    private QuestsDao questsDao;
    private SpheresDao spheresDao;

    private Connection connection;
    private StaminaManager staminaManager;
    private SphereManager sphereManager;
    private PickaxeManager pickaxeManager;


    private StaminaManager staminaManager;

    private SphereManager sphereManager;

    @Override
    public void onEnable() {
        this.sphereManager = new SphereManager(this);
        Bukkit.getPluginManager().registerEvents(new SphereListener(sphereManager), this);
        getDataFolder().mkdirs();

        // Initialize stamina with a default max of 100. This value may grow with quests.
        staminaManager = new StaminaManager(100);

        database = new DatabaseManager(this);
        playersDao = new PlayersDao(database);
        pickaxesDao = new PickaxesDao(database);
        questsDao = new QuestsDao(database);
        spheresDao = new SpheresDao(database);

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

        if (database != null) {
            database.close();
        }
    }

    public PlayersDao getPlayersDao() {
        return playersDao;
    }

    public PickaxesDao getPickaxesDao() {
        return pickaxesDao;
    }

    public QuestsDao getQuestsDao() {
        return questsDao;
    }

    public SpheresDao getSpheresDao() {
        return spheresDao;

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

    public StaminaManager getStaminaManager() {
        return staminaManager;
    }

    public SphereManager getSphereManager() {
        return sphereManager;
    }

    public MobSpawner getMobSpawner() {
        return mobSpawner;

    }
}

