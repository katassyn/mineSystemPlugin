package org.maks.mineSystemPlugin;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

import org.maks.mineSystemPlugin.command.LootCommand;
import org.maks.mineSystemPlugin.command.SphereCommand;
import org.maks.mineSystemPlugin.managers.PickaxeManager;
import org.maks.mineSystemPlugin.stamina.StaminaManager;
import org.maks.mineSystemPlugin.sphere.SphereManager;
import org.maks.mineSystemPlugin.sphere.SphereListener;
import org.maks.mineSystemPlugin.listener.BlockBreakListener;
import org.maks.mineSystemPlugin.listener.OreBreakListener;
import org.maks.mineSystemPlugin.tool.ToolListener;
import org.maks.mineSystemPlugin.storage.MySqlStorage;
import org.maks.mineSystemPlugin.SpecialBlockListener;
import org.maks.mineSystemPlugin.CrystalEnchantCommand;
import org.maks.mineSystemPlugin.RepairCommand;
import org.maks.mineSystemPlugin.LootManager;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Main plugin entry point.
 */
public final class MineSystemPlugin extends JavaPlugin {
    private MySqlStorage storage;
    private LootManager lootManager;
    private SphereManager sphereManager;
    private StaminaManager staminaManager;
    private PickaxeManager pickaxeManager;

    private final Map<Location, Integer> blockHits = new HashMap<>();
    private final Map<String, Integer> oreHits = new HashMap<>();
    private int oreCount;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadOreHitConfig();

        int maxStamina = getConfig().getInt("maxStamina", 100);
        staminaManager = new StaminaManager(this, maxStamina, Duration.ofHours(24));
        sphereManager = new SphereManager(this);
        pickaxeManager = new PickaxeManager(this);
        lootManager = new LootManager();

        try {
            String host = getConfig().getString("mysql.host");
            int port = getConfig().getInt("mysql.port");
            String database = getConfig().getString("mysql.database");
            String user = getConfig().getString("mysql.username");
            String pass = getConfig().getString("mysql.password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            storage = new MySqlStorage(url, user, pass);
            lootManager.setProbabilities(storage.loadItems());
            getCommand("loot").setExecutor(new LootCommand(storage, lootManager));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to connect to MySQL", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("repair").setExecutor(new RepairCommand(this));
        getCommand("crystalenchant").setExecutor(new CrystalEnchantCommand());
        getCommand("sphere").setExecutor(new SphereCommand());
        getCommand("spawnsphere").setExecutor(this);

        registerListener(new SpecialBlockListener());
        registerListener(new SphereListener(sphereManager));
        registerListener(new BlockBreakListener(this));
        registerListener(new OreBreakListener(this));
        registerListener(new ToolListener(this));
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        if (sphereManager != null) {
            sphereManager.removeAll();
        }
        if (pickaxeManager != null) {
            pickaxeManager.saveAll();
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

    public String resolveOreId(Block block) {
        return block.getType().name();
    }

    public int decrementBlockHits(Location location, String oreId) {
        int required = oreHits.getOrDefault(oreId.toUpperCase(Locale.ROOT), 1);
        int hits = blockHits.getOrDefault(location, 0) + 1;
        if (hits >= required) {
            blockHits.remove(location);
            return 0;
        }
        blockHits.put(location, hits);
        return required - hits;
    }

    public int incrementOreCount() {
        oreCount++;
        return oreCount;
    }

    private void loadOreHitConfig() {
        oreHits.clear();
        var section = getConfig().getConfigurationSection("oreHits");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                oreHits.put(key.toUpperCase(Locale.ROOT), section.getInt(key));
            }
        }
    }
}

