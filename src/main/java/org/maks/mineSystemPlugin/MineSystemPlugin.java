package org.maks.mineSystemPlugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.command.LootCommand;
import org.maks.mineSystemPlugin.storage.MySqlStorage;

import java.sql.SQLException;
import java.util.logging.Level;
import org.maks.mineSystemPlugin.events.OreMinedEvent;
import org.maks.mineSystemPlugin.events.SphereCompleteEvent;

import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

import org.maks.mineSystemPlugin.tool.ToolListener;
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

import org.maks.mineSystemPlugin.listener.BlockBreakListener;

import java.util.HashMap;
import java.util.Map;


import org.maks.mineSystemPlugin.command.SphereCommand;


/**
 * Main plugin entry point. Registers the repair command and loads the
 * configuration containing the base repair cost.
 */
public final class MineSystemPlugin extends JavaPlugin {
    private MySqlStorage storage;
    private LootManager lootManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
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

    private SpecialBlockListener listener;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("sphere").setExecutor(new SphereCommand());

        // ensure the config file exists
        saveDefaultConfig();

        // register the repair command
        getCommand("repair").setExecutor(new RepairCommand(this));
        listener = new SpecialBlockListener();
        getServer().getPluginManager().registerEvents(listener, this);
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

    /** Mapping of ore id to required hits loaded from configuration. */
    private final Map<String, Integer> oreHits = new HashMap<>();

    /** Tracks remaining hits for blocks currently being mined. */
    private final Map<String, Integer> blockHits = new HashMap<>();

    private NamespacedKey oreKey;

    @Override
    public void onEnable() {
        getCommand("crystalenchant").setExecutor(new CrystalEnchantCommand());

        // Register listeners
        getServer().getPluginManager().registerEvents(new ToolListener(this), this);

        saveDefaultConfig();

        oreKey = new NamespacedKey(this, "oreId");
        loadOreHits();

        // Register events
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);

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
        if (storage != null) {
            storage.close();
        }
    }

    public LootManager getLootManager() {
        return lootManager;
        blockHits.clear();
        oreHits.clear();
    }

    private void loadOreHits() {
        oreHits.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("oreHits");
        if (section == null) {
            getLogger().warning("No oreHits section found in config");
            return;
        }

        for (String key : section.getKeys(false)) {
            oreHits.put(key, section.getInt(key));
        }
    }

    /**
     * Resolves the ore id for a block. If the block has a persistent data entry
     * named "oreId" that value is used; otherwise the material name of the block
     * is returned.
     */
    public String resolveOreId(Block block) {
        if (block.getState() instanceof TileState tile) {
            PersistentDataContainer container = tile.getPersistentDataContainer();
            String id = container.get(oreKey, PersistentDataType.STRING);
            if (id != null) {
                return id;
            }
        }
        return block.getType().name();
    }

    /**
     * Decrements the hit counter for a block at the given location and returns
     * the remaining hits required before the block breaks.
     */
    public int decrementBlockHits(org.bukkit.Location location, String oreId) {
        int maxHits = oreHits.getOrDefault(oreId, 1);
        String key = key(location);
        int remaining = blockHits.getOrDefault(key, maxHits);
        remaining--;
        if (remaining <= 0) {
            blockHits.remove(key);
        } else {
            blockHits.put(key, remaining);
        }
        return remaining;
    }

    private String key(org.bukkit.Location location) {
        return location.getWorld().getName() + ':' + location.getBlockX() + ':' +
                location.getBlockY() + ':' + location.getBlockZ();
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

