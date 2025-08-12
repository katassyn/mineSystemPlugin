package org.maks.mineSystemPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.io.File;

import org.maks.mineSystemPlugin.command.LootCommand;
import org.maks.mineSystemPlugin.command.MineCommand;
import org.maks.mineSystemPlugin.command.SpecialLootCommand;
import org.maks.mineSystemPlugin.command.StaminaCommand;
import org.maks.mineSystemPlugin.managers.PickaxeManager;
import org.maks.mineSystemPlugin.stamina.StaminaManager;
import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.repository.QuestRepository;
import org.maks.mineSystemPlugin.repository.LootRepository;
import org.maks.mineSystemPlugin.repository.SpecialLootRepository;
import org.maks.mineSystemPlugin.repository.PlayerRepository;
import org.maks.mineSystemPlugin.repository.SphereRepository;
import org.maks.mineSystemPlugin.sphere.SphereManager;
import org.maks.mineSystemPlugin.sphere.SphereListener;
import org.maks.mineSystemPlugin.sphere.SphereType;
import org.maks.mineSystemPlugin.listener.BlockBreakListener;
import org.maks.mineSystemPlugin.listener.OreBreakListener;
import org.maks.mineSystemPlugin.tool.ToolListener;
import org.maks.mineSystemPlugin.SpecialBlockListener;
import org.maks.mineSystemPlugin.CrystalEnchantCommand;
import org.maks.mineSystemPlugin.RepairCommand;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.SpecialLootManager;

import java.time.Duration;
import org.maks.mineSystemPlugin.sphere.Tier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.item.CustomItems;

/**
 * Main plugin entry point.
 */
public final class MineSystemPlugin extends JavaPlugin {
    private LootManager lootManager;
    private SphereManager sphereManager;
    private StaminaManager staminaManager;
    private PickaxeManager pickaxeManager;
    private DatabaseManager database;
    private QuestRepository questRepository;
    private LootRepository lootRepository;
    private SpecialLootManager specialLootManager;
    private SpecialLootRepository specialLootRepository;
    private PlayerRepository playerRepository;
    private SphereRepository sphereRepository;
    private Economy economy;

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

    /**
     * Returns the configured durability for a given ore identifier.
     *
     * @param oreId internal ore name
     * @return number of required hits before the ore breaks
     */
    public int getOreDurability(String oreId) {
        return ORE_DURABILITY.getOrDefault(oreId, 1);
    }

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
    private final Map<UUID, Integer> oreCounts = new HashMap<>();
    private final Random random = new Random();

    private static final List<String> BONUS_ITEMS = List.of("ore_I", "ore_II", "ore_III");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createSchematicFolders();
        setupEconomy();
        database = new DatabaseManager(this);
        questRepository = new QuestRepository(database);
        playerRepository = new PlayerRepository(database);
        sphereRepository = new SphereRepository(database);

        staminaManager = new StaminaManager(this, 100, Duration.ofHours(12), questRepository, playerRepository);
        lootManager = new LootManager();
        lootRepository = new LootRepository(database);
        lootManager.setProbabilities(lootRepository.load().join());
        specialLootManager = new SpecialLootManager();
        specialLootRepository = new SpecialLootRepository(database);
        specialLootRepository.loadAll().join().forEach(specialLootManager::setLoot);
        sphereManager = new SphereManager(this, staminaManager, sphereRepository);
        pickaxeManager = new PickaxeManager(this);
        getCommand("loot").setExecutor(new LootCommand(this, lootRepository, lootManager));
        getCommand("specialloot").setExecutor(new SpecialLootCommand(this, specialLootRepository, specialLootManager));

        RepairCommand repairCommand = new RepairCommand(this);
        getCommand("mine_repair").setExecutor(repairCommand);
        registerListener(repairCommand);
        CrystalEnchantCommand ceCommand = new CrystalEnchantCommand(this);
        getCommand("mine_enchant").setExecutor(ceCommand);
        registerListener(ceCommand);
        getCommand("mine").setExecutor(new MineCommand(this, sphereManager));
        getCommand("stamin").setExecutor(new StaminaCommand(staminaManager));
        getCommand("spawnsphere").setExecutor(this);

        registerListener(new SpecialBlockListener(this));
        registerListener(new SphereListener(sphereManager));
        registerListener(new BlockBreakListener(this));
        registerListener(new OreBreakListener(this));
        registerListener(new ToolListener(this));
    }

    /**
     * Ensures that the schematic folder structure exists so that
     * administrators can easily drop sphere schematics in the proper place.
     */
    private void createSchematicFolders() {
        File base = new File(getDataFolder(), "schematics");
        for (SphereType type : SphereType.values()) {
            File folder = new File(base, type.getFolderName());
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    private void setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
            getLogger().severe("Vault plugin not found, disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
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
        if (staminaManager != null) {
            staminaManager.saveAll();
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
                sphereManager.createSphere(player, false, "/spawnsphere");
            }
            return true;
        }
        return false;
    }

    public SphereManager getSphereManager() {
        return sphereManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public SpecialLootManager getSpecialLootManager() {
        return specialLootManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isCustomOre(Material material) {
        return ORE_ITEM_MAP.containsKey(material);
    }

    public String resolveOreId(Block block) {
        Location loc = block.getLocation().toBlockLocation();
        return blockOreTypes.computeIfAbsent(loc, l -> randomOreFor(block.getType()));
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
        location = location.toBlockLocation();
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

    public int incrementOreCount(UUID uuid) {
        int total = oreCounts.getOrDefault(uuid, 0) + 1;
        oreCounts.put(uuid, total);
        return total;
    }

    public void resetOreCount(UUID uuid) {
        oreCounts.remove(uuid);
    }

    public void dropRandomOreReward(Player player, Location loc) {
        int bonus = random.nextInt(3) + 1;
        for (int i = 0; i < bonus; i++) {
            String rewardId = BONUS_ITEMS.get(random.nextInt(BONUS_ITEMS.size()));
            ItemStack reward = CustomItems.get(rewardId);
            if (reward != null) {
                loc.getWorld().dropItemNaturally(loc, reward.clone());
            }
        }
    }

    public void dropRandomOreReward(Player player) {
        dropRandomOreReward(player, player.getLocation());
    }

    public void handleSphereEnd(Player player) {
        dropRandomOreReward(player);
        resetOreCount(player.getUniqueId());
    }

    /**
     * Registers a specific ore variant for a block when a sphere is pasted.
     * This ensures that subsequent hits use the pre-selected ore type instead of
     * rolling a new random variant on first break.
     *
     * @param location block location inside the sphere
     * @param oreId    internal ore identifier (e.g. "BlackSpinel")
     */
    public void registerOre(Location location, String oreId) {
        Location loc = location.toBlockLocation();
        blockOreTypes.put(loc, oreId);
        blockHits.remove(loc);

    }
}

