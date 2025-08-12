package org.maks.mineSystemPlugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

        // Only process main-hand interactions to avoid double firing
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            // When clicking in the air, getItem may be null even if the player is holding the elixir
            item = event.getPlayer().getInventory().getItem(event.getHand());
            if (item == null) {
                return;
            }

        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        String name = ChatColor.stripColor(meta.getDisplayName());
        if (!"Miner Elixir".equalsIgnoreCase(name)) {

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
