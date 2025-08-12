package org.maks.mineSystemPlugin.sphere;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player events relevant to spheres.
 */
public class SphereListener implements Listener {

    private final SphereManager manager;

    public SphereListener(SphereManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (manager.isInsideSphere(event.getEntity().getLocation())) {
            manager.removeSphereOnDeath(event.getEntity());
        }
    }
}
