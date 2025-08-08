package org.maks.mineSystemPlugin.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

/**
 * Event fired when a player completes a mining sphere. This event allows
 * quest systems and other plugins to react to the completion.
 */
public class SphereCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String type;
    private final Map<String, Integer> statistics;

    public SphereCompleteEvent(Player player, String type, Map<String, Integer> statistics) {
        this.player = player;
        this.type = type;
        this.statistics = statistics;
    }

    public Player getPlayer() {
        return player;
    }

    public String getType() {
        return type;
    }

    public Map<String, Integer> getStatistics() {
        return statistics;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
