package org.maks.mineSystemPlugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.item.CustomItems;
import org.maks.mineSystemPlugin.stamina.StaminaManager;

/**
 * Consumes a Miner Elixir on right click to fully restore the player's stamina.
 */
public class MinerElixirListener implements Listener {

    private final StaminaManager stamina;

    public MinerElixirListener(StaminaManager stamina) {
        this.stamina = stamina;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        String id = CustomItems.getId(item);
        if (!"miner_elixir".equals(id)) {
            return;
        }

        Player player = event.getPlayer();
        int max = stamina.getMaxStamina(player.getUniqueId());
        if (stamina.getStamina(player.getUniqueId()) >= max) {
            player.sendMessage("Your stamina is already full.");
            return;
        }

        stamina.refillStamina(player.getUniqueId());
        item.setAmount(item.getAmount() - 1);
        player.sendMessage("Your stamina has been refreshed.");
        event.setCancelled(true);
    }
}
