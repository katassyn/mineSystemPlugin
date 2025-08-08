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
import org.maks.mineSystemPlugin.events.SphereCompleteEvent;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.SpecialLootEntry;
import org.maks.mineSystemPlugin.SpecialLootManager;
import org.maks.mineSystemPlugin.stamina.StaminaManager;

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

    private record HologramData(ArmorStand stand, String name, int max) {}
    private final Map<Location, HologramData> holograms = new HashMap<>();
    private final Map<Location, BukkitTask> hideTasks = new HashMap<>();

    public SphereManager(Plugin plugin, StaminaManager stamina) {
        this.plugin = plugin;
        this.stamina = stamina;
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

            Location tp = findSafeLocation(region, origin.getWorld());
            if (tp != null) {
                player.teleport(tp);
            } else {
                player.teleport(origin.clone().add(0.5, 1, 0.5));
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> removeSphere(player.getUniqueId()), LIFE_TIME_TICKS);
            Sphere sphere = new Sphere(type, region, task, origin.getWorld(), origin, holograms);
            active.put(player.getUniqueId(), sphere);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> spawnConfiguredMobs(schematic.getName(), region, origin.getWorld()), 20L);
            return true;
        } catch (IOException | WorldEditException e) {
            player.sendMessage("Failed to create sphere");
            return false;
        }
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
            ArmorStand stand = world.spawn(blockLoc.clone().add(0.5, 0, 0.5), ArmorStand.class, as -> {
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

    private Location findSafeLocation(Region region, World world) {
        for (BlockVector3 vec : region) {
            Location loc = new Location(world, vec.getX(), vec.getY(), vec.getZ());
            if (world.getBlockAt(loc).getType() == Material.AIR && world.getBlockAt(loc.clone().add(0, 1, 0)).getType() == Material.AIR) {
                return loc.add(0.5, 0, 0.5);
            }
        }
        return null;
    }

    private Location randomFarLocation(World world) {
        int distance = 10000 + random.nextInt(10001); // 10k-20k blocks from spawn
        double angle = random.nextDouble() * 2 * Math.PI;
        int x = (int) (Math.cos(angle) * distance);
        int z = (int) (Math.sin(angle) * distance);
        int y = world.getMaxHeight() / 2;
        return new Location(world, x, y, z);
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
        Sphere sphere = active.remove(uuid);
        if (sphere != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(player.getWorld().getSpawnLocation());
                Bukkit.getPluginManager().callEvent(
                        new SphereCompleteEvent(player, sphere.getType().name(), Map.of())
                );
            }
            sphere.remove();
            clearHolograms(sphere);
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
        removeSphere(player.getUniqueId());
    }

    public void removeAll() {
        Set<UUID> ids = new HashSet<>(active.keySet());
        ids.forEach(this::removeSphere);
    }
}
