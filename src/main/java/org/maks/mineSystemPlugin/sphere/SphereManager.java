package org.maks.mineSystemPlugin.sphere;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.maks.mineSystemPlugin.events.SphereCompleteEvent;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.SpecialLootEntry;
import org.maks.mineSystemPlugin.SpecialLootManager;
import org.maks.mineSystemPlugin.stamina.StaminaManager;
import org.maks.mineSystemPlugin.model.SphereData;
import org.maks.mineSystemPlugin.repository.SphereRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Manages active mining spheres.
 */
public class SphereManager {

    private static final long LIFE_TIME_TICKS = 10L * 60L * 20L; // 10 minutes

    private static final Map<Material, Integer> BASE_ORE_WEIGHTS = Map.of(
            Material.COAL_ORE, 25,
            Material.IRON_ORE, 20,
            Material.LAPIS_ORE, 15,
            Material.REDSTONE_ORE, 12,
            Material.GOLD_ORE, 10,
            Material.EMERALD_ORE, 10,
            Material.DIAMOND_ORE, 8
    );

    private static final Map<Material, Integer> BASE_ORE_WEIGHTS_PREMIUM = Map.of(
            Material.COAL_ORE, 8,
            Material.IRON_ORE, 10,
            Material.LAPIS_ORE, 10,
            Material.REDSTONE_ORE, 12,
            Material.GOLD_ORE, 15,
            Material.EMERALD_ORE, 20,
            Material.DIAMOND_ORE, 25
    );

    private static final Map<Material, OreVariant[]> ORE_VARIANTS = Map.of(
            Material.COAL_ORE, new OreVariant[]{
                    new OreVariant("Hematite", 40),
                    new OreVariant("BlackSpinel", 70),
                    new OreVariant("BlackDiamond", 120)
            },
            Material.IRON_ORE, new OreVariant[]{
                    new OreVariant("Magnetite", 50),
                    new OreVariant("Silver", 90),
                    new OreVariant("Osmium", 140)
            },
            Material.LAPIS_ORE, new OreVariant[]{
                    new OreVariant("Azurite", 60),
                    new OreVariant("Tanzanite", 100),
                    new OreVariant("BlueSapphire", 150)
            },
            Material.REDSTONE_ORE, new OreVariant[]{
                    new OreVariant("Carnelian", 65),
                    new OreVariant("RedSpinel", 115),
                    new OreVariant("PigeonBloodRuby", 175)
            },
            Material.GOLD_ORE, new OreVariant[]{
                    new OreVariant("Pyrite", 75),
                    new OreVariant("YellowTopaz", 125),
                    new OreVariant("YellowSapphire", 180)
            },
            Material.EMERALD_ORE, new OreVariant[]{
                    new OreVariant("Malachite", 90),
                    new OreVariant("Peridot", 130),
                    new OreVariant("TropicheEmerald", 200)
            },
            Material.DIAMOND_ORE, new OreVariant[]{
                    new OreVariant("Danburite", 100),
                    new OreVariant("Goshenite", 175),
                    new OreVariant("Cerussite", 300)
            }
    );

    private static final int MIN_X = -753;
    private static final int MAX_X = -381;
    private static final int MIN_Y = -61;
    private static final int MAX_Y = 143;
    private static final int MIN_Z = -1658;
    private static final int MAX_Z = -1281;
    private static final int SPHERE_SPACING = 10;

    private final Plugin plugin;
    private final Map<UUID, Sphere> active = new HashMap<>();
    private final Random random = new Random();
    private final int maxSpheres;
    private final StaminaManager stamina;
    private final SphereRepository sphereRepository;
    private final Map<UUID, GameMode> previousModes = new HashMap<>();
    private final boolean debug;

    private record HologramData(ArmorStand stand, String name, int max) {}
    private final Map<Location, HologramData> holograms = new HashMap<>();
    private final Map<Location, BukkitTask> hideTasks = new HashMap<>();

    public SphereManager(Plugin plugin, StaminaManager stamina, SphereRepository sphereRepository) {
        this.plugin = plugin;
        this.stamina = stamina;
        this.sphereRepository = sphereRepository;
        this.maxSpheres = 20;
        this.debug = plugin instanceof MineSystemPlugin mine
                && mine.getConfig().getBoolean("debug.toolListener", false);
    }

    /**
     * Spawns a new mining sphere for the given player.
     *
     * @param player  owner of the sphere
     * @param premium whether to use premium ore distributions
     * @return true if the sphere was created
     */
    public boolean createSphere(Player player, boolean premium) {
        return createSphere(player, premium, "unknown");
    }

    /**
     * Spawns a new mining sphere for the given player.
     *
     * @param player  owner of the sphere
     * @param premium whether to use premium ore distributions
     * @param source  debug description of what triggered the spawn
     * @return true if the sphere was created
     */
    public boolean createSphere(Player player, boolean premium, String source) {
        if (active.size() >= maxSpheres) {
            player.sendMessage("Sphere limit reached");
            return false;
        }
        if (active.containsKey(player.getUniqueId())) {
            player.sendMessage("You already have an active sphere");
            return false;
        }
        if (!stamina.hasStamina(player.getUniqueId(), 10)) {
            player.sendMessage("Not enough stamina");
            return false;
        }
        stamina.deductStamina(player.getUniqueId(), 10);

        List<SphereType> options = Arrays.stream(SphereType.values())
                .filter(t -> {
                    File f = new File(plugin.getDataFolder(), "schematics/" + t.getFolderName());
                    File[] s = f.listFiles((dir, name) -> name.endsWith(".schem"));
                    return s != null && s.length > 0;
                })
                .collect(Collectors.toList());
        if (options.isEmpty()) {
            player.sendMessage("No schematics found");
            return false;
        }
        SphereType type = SphereType.random(options);
        File folder = new File(plugin.getDataFolder(), "schematics/" + type.getFolderName());
        File[] schems = folder.listFiles((dir, name) -> name.endsWith(".schem"));
        if (schems == null || schems.length == 0) {
            player.sendMessage("No schematics found");
            return false;
        }
        File schematic = schems[random.nextInt(schems.length)];

        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        if (format == null) {
            player.sendMessage("Unsupported schematic format");
            return false;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
            Clipboard clipboard = reader.read();
            Region clipRegion = clipboard.getRegion();
            BlockVector3 size = clipRegion.getMaximumPoint().subtract(clipRegion.getMinimumPoint());

            BlockVector3 boundsMin = BlockVector3.at(MIN_X, MIN_Y, MIN_Z);
            BlockVector3 boundsMax = BlockVector3.at(MAX_X, MAX_Y, MAX_Z);
            int xRange = boundsMax.getBlockX() - boundsMin.getBlockX() - size.getBlockX();
            int yRange = boundsMax.getBlockY() - boundsMin.getBlockY() - size.getBlockY();
            int zRange = boundsMax.getBlockZ() - boundsMin.getBlockZ() - size.getBlockZ();

            Location origin = null;
            Region region = null;
            BlockVector3 shift = null;
            BlockVector3 pastePos = null;
            for (int attempt = 0; attempt < 100; attempt++) {
                int x = boundsMin.getBlockX() + random.nextInt(xRange + 1);
                int y = boundsMin.getBlockY() + random.nextInt(yRange + 1);
                int z = boundsMin.getBlockZ() + random.nextInt(zRange + 1);
                BlockVector3 min = BlockVector3.at(x, y, z);
                Region candidate = new CuboidRegion(min, min.add(size));
                boolean overlap = false;
                for (Sphere s : active.values()) {
                    if (intersects(s.getRegion(), candidate, SPHERE_SPACING)) {
                        overlap = true;
                        break;
                    }
                }
                if (!overlap) {
                    region = candidate;
                    pastePos = clipboard.getOrigin().add(min.subtract(clipRegion.getMinimumPoint()));
                    shift = pastePos.subtract(clipboard.getOrigin());
                    origin = new Location(player.getWorld(), pastePos.getX(), pastePos.getY(), pastePos.getZ());
                    break;
                }
            }
            if (origin == null || region == null || shift == null) {
                player.sendMessage("No free space for sphere");
                return false;
            }

            Map<BlockVector3, OreVariant> variants = replacePlaceholders(clipboard, premium);
            BlockVector3 goldVec = findGoldBlock(clipboard);
            BlockVector3 diamondVec = findDiamondBlock(clipboard);


            loadRegionChunks(origin.getWorld(), region);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(origin.getWorld()))
                    .build()) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pastePos)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }

            List<ArmorStand> holograms = spawnHolograms(origin.getWorld(), variants, shift);

            populateChests(origin.getWorld(), region, type, schematic.getName());

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> removeSphere(player.getUniqueId()), LIFE_TIME_TICKS);

            List<BukkitTask> warnings = new ArrayList<>();
            warnings.add(Bukkit.getScheduler().runTaskLater(plugin, () ->
                    player.sendTitle(ChatColor.YELLOW + "5 minutes remaining", "", 10, 70, 20),
                    LIFE_TIME_TICKS / 2));
            warnings.add(Bukkit.getScheduler().runTaskLater(plugin, () ->
                    player.sendTitle(ChatColor.YELLOW + "1 minute remaining", "", 10, 70, 20),
                    LIFE_TIME_TICKS - 60L * 20L));
            warnings.add(new BukkitRunnable() {
                int remaining = 10;

                @Override
                public void run() {
                    if (remaining <= 0) {
                        cancel();
                        return;
                    }
                    player.sendTitle(ChatColor.RED + String.valueOf(remaining), "", 0, 20, 0);
                    remaining--;
                }
            }.runTaskTimer(plugin, LIFE_TIME_TICKS - 200L, 20L));

            Sphere sphere = new Sphere(type, region, task, origin.getWorld(), origin, holograms, warnings, schematic.getName());
            active.put(player.getUniqueId(), sphere);
            sphereRepository.save(new SphereData(player.getUniqueId(), type.name(), System.currentTimeMillis()));
            Location teleport;
            if (goldVec != null) {
                BlockVector3 t = goldVec.add(shift);
                teleport = new Location(origin.getWorld(), t.getX() + 0.5, t.getY() + 1, t.getZ() + 0.5);
            } else {
                teleport = origin.clone().add(0.5, 1, 0.5);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(teleport);
                handleMove(player, teleport);
            }, 40L);
            Location bossLoc = null;
            if (diamondVec != null) {
                BlockVector3 d = diamondVec.add(shift);
                bossLoc = new Location(origin.getWorld(), d.getX() + 0.5, d.getY() + 1, d.getZ() + 0.5);
            }
            Location finalBossLoc = bossLoc;
            Region finalRegion = region;
            Location finalOrigin = origin;
            plugin.getLogger().info("[SphereManager] Scheduling mob spawn for " + schematic.getName());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("[SphereManager] Running mob spawn for " + schematic.getName());
                spawnConfiguredMobs(schematic.getName(), finalRegion, finalOrigin.getWorld(),
                        player, finalBossLoc);
            }, 20L);

            if (schematic.getName().equals("special1.schem") || schematic.getName().equals("special2.schem")) {
                int selectId = schematic.getName().equals("special1.schem") ? 61 : 62;
                if (finalBossLoc != null) {
                    Location npcLoc = finalBossLoc;

                    Location finalOrigin1 = origin;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        String worldName = finalOrigin1.getWorld().getName();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc select " + selectId);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc copy");
                        String cmd = String.format(
                                "npc moveto --world %s --x %.1f --y %.1f --z %.1f",
                                worldName, npcLoc.getX(), npcLoc.getY(), npcLoc.getZ());

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }, 60L);
                }
            }
            return true;
        } catch (IOException | WorldEditException e) {
            player.sendMessage("Failed to create sphere");
            return false;
        }
    }

    private void loadRegionChunks(World world, Region region) {
        int minX = region.getMinimumPoint().getBlockX() >> 4;
        int minZ = region.getMinimumPoint().getBlockZ() >> 4;
        int maxX = region.getMaximumPoint().getBlockX() >> 4;
        int maxZ = region.getMaximumPoint().getBlockZ() >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getChunkAt(x, z);
            }
        }
    }

    private boolean intersects(Region a, Region b, int padding) {
        BlockVector3 aMin = a.getMinimumPoint().subtract(padding, padding, padding);
        BlockVector3 aMax = a.getMaximumPoint().add(padding, padding, padding);
        BlockVector3 bMin = b.getMinimumPoint();
        BlockVector3 bMax = b.getMaximumPoint();
        return !(aMax.getBlockX() < bMin.getBlockX()
                || aMin.getBlockX() > bMax.getBlockX()
                || aMax.getBlockY() < bMin.getBlockY()
                || aMin.getBlockY() > bMax.getBlockY()
                || aMax.getBlockZ() < bMin.getBlockZ()
                || aMin.getBlockZ() > bMax.getBlockZ());
    }


    private Map<BlockVector3, OreVariant> replacePlaceholders(Clipboard clipboard, boolean premium) {
        Map<BlockVector3, OreVariant> variants = new HashMap<>();
        Region region = clipboard.getRegion();
        for (BlockVector3 vec : region) {
            BlockState state = clipboard.getBlock(vec);
            if (state.getBlockType().getId().equals("minecraft:white_wool")) {
                Material mat = randomBaseOre(premium);
                Tier tier = Tier.random(premium);
                clipboard.setBlock(vec, BukkitAdapter.asBlockType(mat).getDefaultState());
                OreVariant variant = ORE_VARIANTS.get(mat)[tier.ordinal()];
                variants.put(vec, variant);
            }
        }
        return variants;
    }

    private BlockVector3 findGoldBlock(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        for (BlockVector3 vec : region) {
            BlockState state = clipboard.getBlock(vec);
            if (state.getBlockType().getId().equals("minecraft:gold_block")) {
                return vec;
            }
        }
        return null;
    }

    private BlockVector3 findDiamondBlock(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        for (BlockVector3 vec : region) {
            BlockState state = clipboard.getBlock(vec);
            if (state.getBlockType().getId().equals("minecraft:diamond_block")) {
                return vec;
            }
        }
        return null;
    }

    private List<ArmorStand> spawnHolograms(World world, Map<BlockVector3, OreVariant> variants, BlockVector3 shift) {
        List<ArmorStand> list = new ArrayList<>();
        MineSystemPlugin pluginImpl = (MineSystemPlugin) plugin;
        for (Map.Entry<BlockVector3, OreVariant> entry : variants.entrySet()) {
            BlockVector3 vec = entry.getKey().add(shift);
            OreVariant data = entry.getValue();
            Location blockLoc = new Location(world, vec.getX(), vec.getY(), vec.getZ()).toBlockLocation();
            String display = addSpaces(data.name());
            ArmorStand stand = world.spawn(blockLoc.clone().add(0.5, 1.2, 0.5), ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setGravity(false);
                as.setCustomName(formatName(display, data.durability(), data.durability()));
                as.setCustomNameVisible(false);
            });
            holograms.put(blockLoc, new HologramData(stand, display, data.durability()));
            pluginImpl.registerOre(blockLoc, data.name());
            list.add(stand);
        }
        return list;
    }

    private String addSpaces(String id) {
        return id.replaceAll("(?<=.)([A-Z])", " $1");
    }

    private void populateChests(World world, Region region, SphereType type, String schematicName) {
        MineSystemPlugin pluginImpl = (MineSystemPlugin) plugin;
        SpecialLootManager special = pluginImpl.getSpecialLootManager();
        List<SpecialLootEntry> specialLoot = special.getLoot(schematicName);
        LootManager loot = pluginImpl.getLootManager();
        boolean hasSpecial = specialLoot != null && !specialLoot.isEmpty();
        boolean treasure = type == SphereType.TREASURE && !hasSpecial;
        for (BlockVector3 vec : region) {
            Block block = world.getBlockAt(vec.getX(), vec.getY(), vec.getZ());
            if (block.getState() instanceof Chest chest) {
                if (hasSpecial) {
                    special.fillInventory(schematicName, chest.getBlockInventory());
                } else if (treasure) {
                    loot.fillInventory(chest.getBlockInventory());
                }
            }
        }
    }

    private String formatName(String name, int remaining, int max) {
        return ChatColor.YELLOW + name + " " + ChatColor.RED + remaining + "/" + max + " " + progressBar(remaining, max);
    }

    private String progressBar(int remaining, int max) {
        int bars = 10;
        int filled = (int) Math.round((double) remaining / max * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? ChatColor.GREEN : ChatColor.DARK_GRAY).append("|");
        }
        return sb.toString();
    }

    public void updateHologram(Location loc, String oreId, int remaining) {
        loc = loc.toBlockLocation();
        HologramData data = holograms.get(loc);
        if (data == null) {
            MineSystemPlugin pluginImpl = (MineSystemPlugin) plugin;
            int max = pluginImpl.getOreDurability(oreId);
            String display = addSpaces(oreId);
            ArmorStand stand = loc.getWorld().spawn(loc.clone().add(0.5, 1.2, 0.5), ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setGravity(false);
            });
            data = new HologramData(stand, display, max);
            holograms.put(loc, data);
        }
        if (remaining <= 0) {
            BukkitTask task = hideTasks.remove(loc);
            if (task != null) {
                task.cancel();
            }
            data.stand().remove();
            holograms.remove(loc);
            return;
        }
        data.stand().setCustomName(formatName(data.name(), remaining, data.max()));
        data.stand().setCustomNameVisible(true);
        BukkitTask task = hideTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }
        Location finalLoc = loc;
        HologramData finalData = data;
        hideTasks.put(loc, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalData.stand().setCustomNameVisible(false);
            hideTasks.remove(finalLoc);
        }, 60L));
    }

    private void spawnConfiguredMobs(String schematic, Region region, World world,
                                     Player player, Location bossLoc) {
        String key = "mobs." + schematic.replace(".", "\\.");
        plugin.getLogger().info("[SphereManager] Loading mob config at key: " + key);
        List<Map<?, ?>> entries = ((JavaPlugin) plugin).getConfig().getMapList(key);
        if (entries.isEmpty()) {
            plugin.getLogger().warning("[SphereManager] No entries found via getMapList, attempting fallback");
            ConfigurationSection section = ((JavaPlugin) plugin).getConfig().getConfigurationSection("mobs");
            if (section != null) {
                Object raw = section.get(schematic);
                if (raw instanceof List<?>) {
                    //noinspection unchecked
                    entries = (List<Map<?, ?>>) raw;
                }
            }
        }
        plugin.getLogger().info("[SphereManager] Found " + entries.size() + " mob entries");
        for (Map<?, ?> entry : entries) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) entry;
            String mythic = (String) map.get("mythic_id");
            Number amtNum = (Number) map.getOrDefault("amount", 1);
            int amount = amtNum.intValue();
            boolean boss = Boolean.TRUE.equals(map.get("boss"));
            plugin.getLogger().info("[SphereManager] Spawning " + amount + " of " + mythic + (boss ? " (boss)" : ""));
            for (int i = 0; i < amount; i++) {
                Location loc = boss && bossLoc != null
                        ? bossLoc
                        : randomSpawnNearPlayer(region, world, player);
                if (loc != null && mythic != null) {
                    String cmd = String.format("mm m spawn %s 1 %s,%.1f,%.1f,%.1f",
                            mythic, world.getName(), loc.getX(), loc.getY(), loc.getZ());
                    plugin.getLogger().info("[SphereManager] Dispatching command: " + cmd);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } else {
                    plugin.getLogger().warning("[SphereManager] Missing location or mythic id for spawn");
                }
            }
        }
    }

    private Location randomSpawnNearPlayer(Region region, World world, Player player) {
        Location base = player.getLocation();
        Vector dir = base.getDirection().setY(0).normalize();
        for (int i = 0; i < 40; i++) {
            double dist = 2 + random.nextDouble() * 2; // 2-4 blocks ahead
            double angle = (random.nextDouble() - 0.5) * Math.PI / 3; // +/-30 degrees
            Vector offset = dir.clone().rotateAroundY(angle).multiply(dist);
            int x = base.getBlockX() + (int) Math.round(offset.getX());
            int z = base.getBlockZ() + (int) Math.round(offset.getZ());
            int y = base.getBlockY();
            if (!region.contains(BlockVector3.at(x, y, z))) {
                continue;
            }
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block below = world.getBlockAt(x, y - 1, z);
            Block twoAbove = world.getBlockAt(x, y + 2, z);
            if (block.getType() == Material.AIR && above.getType() == Material.AIR && below.getType().isSolid()
                    && twoAbove.getType().isSolid()) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private Material randomBaseOre(boolean premium) {
        Map<Material, Integer> weights = premium ? BASE_ORE_WEIGHTS_PREMIUM : BASE_ORE_WEIGHTS;
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int r = random.nextInt(total);
        int cumulative = 0;
        for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (r < cumulative) {
                return entry.getKey();
            }
        }
        return Material.COAL_ORE;
    }

    public void handleMove(Player player, Location to) {
        Sphere sphere = active.get(player.getUniqueId());
        if (sphere == null) {
            return;
        }
        boolean inside = sphere.getRegion().contains(BlockVector3.at(to.getBlockX(), to.getBlockY(), to.getBlockZ()));
        if (inside) {
            if (!previousModes.containsKey(player.getUniqueId())) {
                previousModes.put(player.getUniqueId(), player.getGameMode());
                player.setGameMode(GameMode.SURVIVAL);
                player.sendTitle(ChatColor.GOLD + sphere.getType().getDisplayName(), "", 10, 70, 20);
                if (debug) {
                    plugin.getLogger().info("[SphereManager] Set " + player.getName() + " to SURVIVAL inside sphere");
                }
            }
        } else {
            restoreGameMode(player);
        }
    }

    public void restoreGameMode(Player player) {
        GameMode prev = previousModes.remove(player.getUniqueId());
        if (prev != null) {
            player.setGameMode(prev);
            if (debug) {
                plugin.getLogger().info("[SphereManager] Restored " + player.getName() + " to " + prev);
            }
        }
    }

    public void removeSphere(UUID uuid) {
        removeSphere(uuid, Bukkit.getPlayer(uuid));
    }

    public void removeSphere(Player player) {
        removeSphere(player.getUniqueId(), player);
    }

    public void removeSphereOnDeath(Player player) {
        removeSphere(player.getUniqueId(), null);
    }

    private void removeSphere(UUID uuid, Player player) {
        Sphere sphere = active.remove(uuid);
        if (sphere != null) {
            Player p = player != null ? player : Bukkit.getPlayer(uuid);
            if (p != null) {
                restoreGameMode(p);
            }
            if (plugin instanceof MineSystemPlugin mine) {
                if (p != null) {
                    mine.handleSphereEnd(p);
                } else {
                    mine.resetOreCount(uuid);
                }
            }
            if (p != null) {
                p.sendTitle(ChatColor.RED + "Time's up!",
                        ChatColor.RED + "Returning to spawn", 10, 70, 20);
                Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essentials != null && essentials.isEnabled()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
                } else {
                    p.teleport(p.getWorld().getSpawnLocation());
                }

                Bukkit.getPluginManager().callEvent(
                        new SphereCompleteEvent(p, sphere.getType().name(), Map.of())
                );
            }
            sphere.remove();
            if ("special1.schem".equals(sphere.getSchematicName()) || "special2.schem".equals(sphere.getSchematicName())) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc rem");
            }
            clearHolograms(sphere);
            sphereRepository.save(new SphereData(uuid, sphere.getType().name(), 0L));
        }
    }

    private void clearHolograms(Sphere sphere) {
        Region region = sphere.getRegion();
        World world = sphere.getWorld();
        holograms.keySet().removeIf(loc -> {
            if (!loc.getWorld().equals(world)) {
                return false;
            }
            boolean inside = region.contains(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            if (inside) {
                BukkitTask task = hideTasks.remove(loc);
                if (task != null) {
                    task.cancel();
                }
            }
            return inside;
        });
    }

    public void onPlayerQuit(Player player) {
        removeSphere(player);
    }

    public void removeAll() {
        Set<UUID> ids = new HashSet<>(active.keySet());
        ids.forEach(this::removeSphere);
    }

    public boolean isInsideSphere(Location loc) {
        BlockVector3 vec = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        for (Sphere sphere : active.values()) {
            if (sphere.getRegion().contains(vec)) {
                return true;
            }
        }
        return false;
    }
}
