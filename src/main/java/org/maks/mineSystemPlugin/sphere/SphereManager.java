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
import org.bukkit.Material;
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

    private static final long LIFE_TIME_TICKS = 10L * 60L * 20L; // 10 minutes

    private final Plugin plugin;
    private final Map<UUID, Sphere> active = new HashMap<>();
    private final Random random = new Random();
    private final int maxSpheres;
    private final List<Material> oreMaterials = new ArrayList<>();

    public SphereManager(Plugin plugin) {
        this.plugin = plugin;
        this.maxSpheres = plugin.getConfig().getInt("sphereLimit", 20);
        List<String> configured = plugin.getConfig().getStringList("sphereOres");
        for (String name : configured) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                oreMaterials.add(mat);
            }
        }
        if (oreMaterials.isEmpty()) {
            oreMaterials.addAll(Arrays.asList(
                    Material.COAL_ORE,
                    Material.IRON_ORE,
                    Material.LAPIS_ORE,
                    Material.REDSTONE_ORE,
                    Material.GOLD_ORE,
                    Material.EMERALD_ORE,
                    Material.DIAMOND_ORE
            ));
        }
    }

    public boolean createSphere(Player player) {
        if (active.size() >= maxSpheres) {
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
            if (state.getBlockType().getId().equals("minecraft:white_wool")) {
                Material mat = oreMaterials.get(random.nextInt(oreMaterials.size()));
                clipboard.setBlock(vec, BukkitAdapter.adapt(mat).createBlockState());
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
    }
}
