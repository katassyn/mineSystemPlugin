package org.maks.mineSystemPlugin.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired whenever a player mines an ore. This can be used by quest
 * systems and other plugins to track mining progress.
 */
public class OreMinedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Material oreType;
    private final int amount;
    private final int pickaxeLevel;

    public OreMinedEvent(Player player, Material oreType, int amount, int pickaxeLevel) {
        this.player = player;
        this.oreType = oreType;
        this.amount = amount;
        this.pickaxeLevel = pickaxeLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public Material getOreType() {
        return oreType;
    }

    public int getAmount() {
        return amount;
    }

    public int getPickaxeLevel() {
        return pickaxeLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
