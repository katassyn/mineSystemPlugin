package org.maks.mineSystemPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.command.LootCommand;
import org.maks.mineSystemPlugin.storage.MySqlStorage;

import java.sql.SQLException;
import java.util.logging.Level;

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
    }
}
