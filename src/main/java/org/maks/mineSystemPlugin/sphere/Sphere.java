package org.maks.mineSystemPlugin.sphere;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Represents a pasted sphere instance in the world.
 */
public class Sphere {

    private final SphereType type;
    private final Region region;
    private final BukkitTask expiryTask;
    private final World world;
    private final Location origin;
    private final List<ArmorStand> holograms;

    public Sphere(SphereType type, Region region, BukkitTask expiryTask, World world, Location origin, List<ArmorStand> holograms) {
        this.type = type;
        this.region = region;
        this.expiryTask = expiryTask;
        this.world = world;
        this.origin = origin;
        this.holograms = holograms;
    }

    public SphereType getType() {
        return type;
    }

    public Region getRegion() {
        return region;
    }

    public World getWorld() {
        return world;
    }

    /**
     * Removes the sphere by setting its region to air and cancelling timers.
     */
    public void remove() {
        expiryTask.cancel();
        for (ArmorStand stand : holograms) {
            stand.remove();
        }
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            Location l = entity.getLocation();
            if (region.contains(BlockVector3.at(l.getBlockX(), l.getBlockY(), l.getBlockZ()))) {
                entity.remove();
            }
        }
        try (EditSession session = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .build()) {
            for (BlockVector3 vec : region) {
                session.setBlock(vec, BlockTypes.AIR.getDefaultState());
            }
        } catch (WorldEditException e) {
            // Swallow exception to avoid plugin crash
        }
    }

    public Location getOrigin() {
        return origin.clone();
    }
}
