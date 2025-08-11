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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
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
                    new OreVariant("Black Spinel", 70),
                    new OreVariant("Black Diamond", 120)
            },
            Material.IRON_ORE, new OreVariant[]{
                    new OreVariant("Magnetite", 50),
                    new OreVariant("Silver", 90),
                    new OreVariant("Osmium", 140)
            },
            Material.LAPIS_ORE, new OreVariant[]{
                    new OreVariant("Azurite", 60),
                    new OreVariant("Tanzanite", 100),
                    new OreVariant("Blue Sapphire", 150)
            },
            Material.REDSTONE_ORE, new OreVariant[]{
                    new OreVariant("Carnelian", 65),
                    new OreVariant("Red Spinel", 115),
                    new OreVariant("Pigeon Blood Ruby", 175)
            },
            Material.GOLD_ORE, new OreVariant[]{
                    new OreVariant("Pyrite", 75),
                    new OreVariant("Yellow Topaz", 125),
                    new OreVariant("Yellow Sapphire", 180)
            },
            Material.EMERALD_ORE, new OreVariant[]{
                    new OreVariant("Malachite", 90),
                    new OreVariant("Peridot", 130),
                    new OreVariant("Tropiche Emerald", 200)
            },
            Material.DIAMOND_ORE, new OreVariant[]{
                    new OreVariant("Danburite", 100),
                    new OreVariant("Goshenite", 175),
                    new OreVariant("Cerussite", 300)
            }
    );

    private final Plugin plugin;
    private final Map<UUID, Sphere> active = new HashMap<>();
    private final Random random = new Random();
    private final int maxSpheres;
    private final StaminaManager stamina;
    private final SphereRepository sphereRepository;

    private record HologramData(ArmorStand stand, String name, int max) {}
    private final Map<Location, HologramData> holograms = new HashMap<>();
    private final Map<Location, BukkitTask> hideTasks = new HashMap<>();

    public SphereManager(Plugin plugin, StaminaManager stamina, SphereRepository sphereRepository) {
        this.plugin = plugin;
        this.stamina = stamina;
        this.sphereRepository = sphereRepository;
        this.maxSpheres = 20;
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

        SphereType type = SphereType.random();
        File folder = new File(plugin.getDataFolder(), "schematics/" + type.getFolderName());
        if (!folder.exists()) {
            player.sendMessage("No schematics for type " + type.name());
            return false;
        }
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

        Location origin = randomFarLocation(player.getWorld());
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
            Clipboard clipboard = reader.read();
            Map<BlockVector3, OreVariant> variants = replacePlaceholders(clipboard, premium);
            BlockVector3 goldVec = findGoldBlock(clipboard);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(origin.getWorld()))
                    .build()) {
                Operation operation = new ClipboardHolder(clipboard)

                        .createPaste(editSession)
                        .to(BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()))
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }

            Region clipRegion = clipboard.getRegion();
            BlockVector3 offset = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
            Region region = new CuboidRegion(
                    clipRegion.getMinimumPoint().add(offset),
                    clipRegion.getMaximumPoint().add(offset));

            List<ArmorStand> holograms = spawnHolograms(origin, variants);

            populateChests(origin.getWorld(), region, type, schematic.getName());

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> removeSphere(player.getUniqueId()), LIFE_TIME_TICKS);

            List<BukkitTask> warnings = new ArrayList<>();
            warnings.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendTitle(ChatColor.YELLOW + "5 minutes remaining", "", 10, 70, 20);
                player.sendMessage(ChatColor.YELLOW + "5 minutes remaining");
            }, LIFE_TIME_TICKS / 2));
            warnings.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendTitle(ChatColor.YELLOW + "1 minute remaining", "", 10, 70, 20);
                player.sendMessage(ChatColor.YELLOW + "1 minute remaining");
            }, LIFE_TIME_TICKS - 60L * 20L));
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

            Sphere sphere = new Sphere(type, region, task, origin.getWorld(), origin, holograms, warnings);
            active.put(player.getUniqueId(), sphere);
            sphereRepository.save(new SphereData(player.getUniqueId(), type.name(), System.currentTimeMillis()));
            Location teleport = origin.clone();
            if (goldVec != null) {
                teleport.add(goldVec.getX(), goldVec.getY(), goldVec.getZ());
            }
            teleport.add(0.5, 1, 0.5);
            plugin.getLogger().info(String.format("Spawned %s sphere for %s via %s at %d %d %d",
                    type.name(), player.getName(), source,
                    teleport.getBlockX(), teleport.getBlockY() - 1, teleport.getBlockZ()));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(teleport);
                String coords = String.format("%d %d %d", teleport.getBlockX(), teleport.getBlockY() - 1, teleport.getBlockZ());
                player.sendMessage(ChatColor.YELLOW + "Teleported to sphere at " + coords);
            }, 40L);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> spawnConfiguredMobs(schematic.getName(), region, origin.getWorld()), 20L);
            return true;
        } catch (IOException | WorldEditException e) {
            player.sendMessage("Failed to create sphere");
            return false;
        }
    }

    private Location randomFarLocation(World world) {
        int distance = 10000 + random.nextInt(10001);
        double angle = random.nextDouble() * 2 * Math.PI;
        int x = (int) (Math.cos(angle) * distance);
        int z = (int) (Math.sin(angle) * distance);
        int y = world.getMaxHeight() / 2;
        return new Location(world, x, y, z);
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

    private List<ArmorStand> spawnHolograms(Location origin, Map<BlockVector3, OreVariant> variants) {
        List<ArmorStand> list = new ArrayList<>();
        World world = origin.getWorld();
        for (Map.Entry<BlockVector3, OreVariant> entry : variants.entrySet()) {
            BlockVector3 vec = entry.getKey();
            OreVariant data = entry.getValue();
            Location blockLoc = new Location(world,
                    origin.getBlockX() + vec.getX(),
                    origin.getBlockY() + vec.getY(),
                    origin.getBlockZ() + vec.getZ());
            ArmorStand stand = world.spawn(blockLoc.clone().add(0.5, 1.2, 0.5), ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setGravity(false);
                as.setCustomName(formatName(data.name(), data.durability(), data.durability()));
                as.setCustomNameVisible(false);
            });
            holograms.put(blockLoc, new HologramData(stand, data.name(), data.durability()));
            list.add(stand);
        }
        return list;
    }

    private void populateChests(World world, Region region, SphereType type, String schematicName) {
        MineSystemPlugin pluginImpl = (MineSystemPlugin) plugin;
        SpecialLootManager special = pluginImpl.getSpecialLootManager();
        Map<Material, SpecialLootEntry> specialLoot = special.getLoot(schematicName);
        LootManager loot = pluginImpl.getLootManager();
        boolean treasure = type == SphereType.TREASURE && (specialLoot == null || specialLoot.isEmpty());
        for (BlockVector3 vec : region) {
            Block block = world.getBlockAt(vec.getX(), vec.getY(), vec.getZ());
            if (block.getState() instanceof Chest chest) {
                if (specialLoot != null && !specialLoot.isEmpty()) {
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

    public void updateHologram(Location loc, int remaining) {
        HologramData data = holograms.get(loc);
        if (data == null) {
            return;
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
        hideTasks.put(loc, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            data.stand().setCustomNameVisible(false);
            hideTasks.remove(loc);
        }, 60L));
    }

    private void spawnConfiguredMobs(String schematic, Region region, World world) {
        List<Map<?, ?>> entries = ((JavaPlugin) plugin).getConfig().getMapList("mobs." + schematic);
        for (Map<?, ?> entry : entries) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) entry;
            String mythic = (String) map.get("mythic_id");
            Number amtNum = (Number) map.getOrDefault("amount", 1);
            int amount = amtNum.intValue();
            for (int i = 0; i < amount; i++) {
                Location loc = randomSpawnLocation(region, world);
                if (loc != null && mythic != null) {
                    String cmd = String.format("mythicmobs spawn %s %s %.1f %.1f %.1f", mythic,
                            world.getName(), loc.getX(), loc.getY(), loc.getZ());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
    }

    private Location randomSpawnLocation(Region region, World world) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        for (int i = 0; i < 20; i++) {
            int x = min.getBlockX() + random.nextInt(max.getBlockX() - min.getBlockX() + 1);
            int y = min.getBlockY() + random.nextInt(max.getBlockY() - min.getBlockY() + 1);
            int z = min.getBlockZ() + random.nextInt(max.getBlockZ() - min.getBlockZ() + 1);
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block below = world.getBlockAt(x, y - 1, z);
            if (block.getType() == Material.AIR && above.getType() == Material.AIR && below.getType().isSolid()) {
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

    public void removeSphere(UUID uuid) {
        removeSphere(uuid, Bukkit.getPlayer(uuid));
    }

    public void removeSphere(Player player) {
        removeSphere(player.getUniqueId(), player);
    }

    private void removeSphere(UUID uuid, Player player) {
        Sphere sphere = active.remove(uuid);
        if (sphere != null) {
            if (player != null) {
                player.sendTitle(ChatColor.RED + "Time's up!", "", 10, 70, 20);
                player.sendMessage(ChatColor.RED + "Sphere expired. Returning to spawn.");
                Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essentials != null && essentials.isEnabled()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + player.getName());
                } else {
                    player.teleport(player.getWorld().getSpawnLocation());
                }
                Bukkit.getPluginManager().callEvent(
                        new SphereCompleteEvent(player, sphere.getType().name(), Map.of())
                );
            }
            sphere.remove();
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
