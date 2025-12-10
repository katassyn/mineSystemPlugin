package org.maks.mineSystemPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.events.OreMinedEvent;

import java.lang.reflect.Method;

/**
 * Integration listener for Miner Day event from EventPlugin.
 * Adds progress to the miner_day event when ores are mined.
 */
public class MinerDayIntegrationListener implements Listener {
    private final MineSystemPlugin plugin;
    private Class<?> eventPluginAPIClass;
    private Method isEventActiveMethod;
    private Method getEventManagerMethod;
    private Method addProgressMethod;
    private boolean integrationAvailable = false;

    public MinerDayIntegrationListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
        initializeIntegration();
    }

    private void initializeIntegration() {
        try {
            // Try to load EventPluginAPI class
            eventPluginAPIClass = Class.forName("org.maks.eventPlugin.api.EventPluginAPI");
            isEventActiveMethod = eventPluginAPIClass.getMethod("isEventActive", String.class);
            getEventManagerMethod = eventPluginAPIClass.getMethod("getEventManager", String.class);

            // Get EventManager class and its addProgress method
            Class<?> eventManagerClass = Class.forName("org.maks.eventPlugin.eventsystem.EventManager");
            addProgressMethod = eventManagerClass.getMethod("addProgress", Player.class, int.class, double.class);

            integrationAvailable = true;
            plugin.getLogger().info("[MinerDay Integration] EventPlugin API loaded successfully");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            plugin.getLogger().info("[MinerDay Integration] EventPlugin not found - miner_day event integration disabled");
        }
    }

    /**
     * Check if miner_day event is currently active.
     */
    public boolean isMinerDayActive() {
        if (!integrationAvailable) {
            return false;
        }

        try {
            return (boolean) isEventActiveMethod.invoke(null, "miner_day");
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreMined(OreMinedEvent event) {
        if (!integrationAvailable || !isMinerDayActive()) {
            return;
        }

        try {
            // Get the event manager for miner_day
            Object eventManager = getEventManagerMethod.invoke(null, "miner_day");
            if (eventManager == null) {
                return;
            }

            // Add 1 progress for each ore mined (not the amount dropped, just 1 per ore block)
            addProgressMethod.invoke(eventManager, event.getPlayer(), 1, 1.0);
        } catch (Exception e) {
            plugin.getLogger().warning("[MinerDay Integration] Failed to add progress: " + e.getMessage());
        }
    }
}
