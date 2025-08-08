package org.maks.mineSystemPlugin.sphere;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.EditSession;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Manages active mining spheres.
 */
public class SphereManager {

    private static final int MAX_SPHERES = 20;
    private static final long LIFE_TIME_TICKS = 10L * 60L * 20L; // 10 minutes

    private final Plugin plugin;
    private final Map<UUID, Sphere> active = new HashMap<>();
    private final Random random = new Random();

    public SphereManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean createSphere(Player player) {
        if (active.size() >= MAX_SPHERES) {
            player.sendMessage("Sphere limit reached");
            return false;
        }
        if (active.containsKey(player.getUniqueId())) {
            player.sendMessage("You already have an active sphere");
            return false;
        }

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

        Location origin = player.getLocation();
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
            Clipboard clipboard = reader.read();
            replacePlaceholders(clipboard);

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(origin.getWorld()))) {
                Operation operation = new com.sk89q.worldedit.extent.clipboard.ClipboardHolder(clipboard)
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

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> removeSphere(player.getUniqueId()), LIFE_TIME_TICKS);
            Sphere sphere = new Sphere(type, region, task, origin.getWorld(), origin);
            active.put(player.getUniqueId(), sphere);
            return true;
        } catch (IOException | WorldEditException e) {
            player.sendMessage("Failed to create sphere");
            return false;
        }
    }

    private void replacePlaceholders(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        for (BlockVector3 vec : region) {
            BlockState state = clipboard.getBlock(vec);
            String id = state.getBlockType().getId();
            if (id.endsWith("_wool")) {
                OreType ore = OreType.random();
                Tier.random(); // tier is chosen but not yet used
                clipboard.setBlock(vec, BukkitAdapter.adapt(ore.getMaterial()).createBlockState());
            }
        }
    }

    public void removeSphere(UUID uuid) {
        Sphere sphere = active.remove(uuid);
        if (sphere != null) {
            sphere.remove();
        }
    }

    public void onPlayerQuit(Player player) {
        removeSphere(player.getUniqueId());
    }

    public void removeAll() {
        Set<UUID> ids = new HashSet<>(active.keySet());
        ids.forEach(this::removeSphere);

import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.mob.MobSpawner;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks active spheres and enforces a global limit.
 */
public class SphereManager {
    private final MineSystemPlugin plugin;
    private final int limit;
    private final Set<UUID> active = new HashSet<>();
    private final MobSpawner mobSpawner;

    /** Placeholder block replaced by actual ores when generating spheres. */
    public static final Material ORE_PLACEHOLDER = Material.WHITE_WOOL;

    public SphereManager(MineSystemPlugin plugin, int limit, MobSpawner mobSpawner) {
        this.plugin = plugin;
        this.limit = limit;
        this.mobSpawner = mobSpawner;
    }

    public boolean canCreateSphere(Player player) {
        if (active.size() >= limit) {
            player.sendMessage("Too many active spheres. Please wait.");
            return false;
        }
        return true;
    }

    public void registerSphere(Player player) {
        active.add(player.getUniqueId());
        Location center = player.getLocation();
        mobSpawner.spawn(center);
    }

    public void removeSphere(Player player) {
        active.remove(player.getUniqueId());
    }
}
