package org.maks.mineSystemPlugin.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles the creation of spheres via the /sphere command. When a player
 * attempts to create a sphere of type 2 they must possess an item named
 * "Premium Ticket". One instance of the item is consumed; otherwise the
 * attempt is rejected.
 */
public class SphereCommand implements CommandExecutor {

    private static final String PREMIUM_TICKET_NAME = "Premium Ticket";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <type>");
            return true;
        }

        int type;
        try {
            type = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Type must be a number.");
            return true;
        }

        if (type == 2) {
            if (!consumePremiumTicket(player)) {
                player.sendMessage(ChatColor.RED + "You need a premium ticket to create this sphere!");
                return true;
            }
        }

        // Sphere creation logic would go here
        player.sendMessage(ChatColor.GREEN + "Sphere type " + type + " created.");
        return true;
    }

    /**
     * Looks for the named ticket in the player's inventory and removes one
     * instance if found.
     *
     * @param player Player whose inventory is inspected
     * @return true if an item was consumed, false otherwise
     */
    private boolean consumePremiumTicket(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null) continue;
            if (stack.getType() != Material.PAPER) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;
            String name = ChatColor.stripColor(meta.getDisplayName());
            if (PREMIUM_TICKET_NAME.equalsIgnoreCase(name)) {
                int amount = stack.getAmount();
                if (amount > 1) {
                    stack.setAmount(amount - 1);
                } else {
                    inventory.setItem(slot, null);
                }
                return true;
            }
        }
        return false;
    }
}

