package org.maks.mineSystemPlugin.sphere;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.EditSession;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a pasted sphere instance in the world.
 */
public class Sphere {

    private final SphereType type;
    private final Region region;
    private final BukkitTask expiryTask;
    private final World world;
    private final Location origin;

    public Sphere(SphereType type, Region region, BukkitTask expiryTask, World world, Location origin) {
        this.type = type;
        this.region = region;
        this.expiryTask = expiryTask;
        this.world = world;
        this.origin = origin;
    }

    public SphereType getType() {
        return type;
    }

    /**
     * Removes the sphere by setting its region to air and cancelling timers.
     */
    public void remove() {
        expiryTask.cancel();
        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
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
