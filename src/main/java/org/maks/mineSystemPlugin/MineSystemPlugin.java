package org.maks.mineSystemPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.maks.mineSystemPlugin.command.LootCommand;
import org.maks.mineSystemPlugin.command.SphereCommand;
import org.maks.mineSystemPlugin.command.MineCommand;
import org.maks.mineSystemPlugin.managers.PickaxeManager;
import org.maks.mineSystemPlugin.stamina.StaminaManager;
import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.repository.QuestRepository;
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
import org.maks.mineSystemPlugin.sphere.Tier;

import java.util.HashMap;
import java.util.List;
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
    private DatabaseManager database;
    private QuestRepository questRepository;

    private static final Map<String, Integer> ORE_DURABILITY = Map.ofEntries(
            Map.entry("Hematite", 40),
            Map.entry("BlackSpinel", 70),
            Map.entry("BlackDiamond", 120),
            Map.entry("Magnetite", 50),
            Map.entry("Silver", 90),
            Map.entry("Osmium", 140),
            Map.entry("Azurite", 60),
            Map.entry("Tanzanite", 100),
            Map.entry("BlueSapphire", 150),
            Map.entry("Carnelian", 65),
            Map.entry("RedSpinel", 115),
            Map.entry("PigeonBloodRuby", 175),
            Map.entry("Pyrite", 75),
            Map.entry("YellowTopaz", 125),
            Map.entry("YellowSapphire", 180),
            Map.entry("Malachite", 90),
            Map.entry("Peridot", 130),
            Map.entry("TropicheEmerald", 200),
            Map.entry("Danburite", 100),
            Map.entry("Goshenite", 175),
            Map.entry("Cerussite", 300)
    );

    private static final Map<Material, List<String>> ORE_ITEM_MAP = Map.of(
            Material.COAL_ORE, List.of("Hematite", "BlackSpinel", "BlackDiamond"),
            Material.IRON_ORE, List.of("Magnetite", "Silver", "Osmium"),
            Material.LAPIS_ORE, List.of("Azurite", "Tanzanite", "BlueSapphire"),
            Material.REDSTONE_ORE, List.of("Carnelian", "RedSpinel", "PigeonBloodRuby"),
            Material.GOLD_ORE, List.of("Pyrite", "YellowTopaz", "YellowSapphire"),
            Material.EMERALD_ORE, List.of("Malachite", "Peridot", "TropicheEmerald"),
            Material.DIAMOND_ORE, List.of("Danburite", "Goshenite", "Cerussite")
    );

    private final Map<Location, Integer> blockHits = new HashMap<>();
    private final Map<Location, String> blockOreTypes = new HashMap<>();
    private int oreCount;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        database = new DatabaseManager(this);
        questRepository = new QuestRepository(database);

        int maxStamina = getConfig().getInt("maxStamina", 100);
        staminaManager = new StaminaManager(this, maxStamina, Duration.ofHours(12), questRepository);
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
            getCommand("loot").setExecutor(new LootCommand(this, storage, lootManager));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to connect to MySQL", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("repair").setExecutor(new RepairCommand(this));
        CrystalEnchantCommand ceCommand = new CrystalEnchantCommand(this);
        getCommand("crystalenchant").setExecutor(ceCommand);
        registerListener(ceCommand);
        getCommand("sphere").setExecutor(new SphereCommand());
        getCommand("mine").setExecutor(new MineCommand(this));
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
        if (database != null) {
            database.close();
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

    public boolean isCustomOre(Material material) {
        return ORE_ITEM_MAP.containsKey(material);
    }

    public String resolveOreId(Block block) {
        return blockOreTypes.computeIfAbsent(block.getLocation(), loc -> randomOreFor(block.getType()));
    }

    private String randomOreFor(Material material) {
        List<String> ores = ORE_ITEM_MAP.get(material);
        if (ores == null || ores.isEmpty()) {
            return material.name();
        }
        Tier tier = Tier.random();
        return ores.get(tier.ordinal());
    }

    public int decrementBlockHits(Location location, String oreId) {
        int required = ORE_DURABILITY.getOrDefault(oreId, 1);
        int hits = blockHits.getOrDefault(location, 0) + 1;
        if (hits >= required) {
            blockHits.remove(location);
            blockOreTypes.remove(location);
            return 0;
        }
        blockHits.put(location, hits);
        return required - hits;
    }

    public int incrementOreCount() {
        oreCount++;
        return oreCount;
    }
}

