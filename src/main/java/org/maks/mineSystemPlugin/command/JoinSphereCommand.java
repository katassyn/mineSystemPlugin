package org.maks.mineSystemPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maks.mineSystemPlugin.MineSystemPlugin;

/**
 * Simple command for testing stamina and sphere limits.
 */
public class JoinSphereCommand implements CommandExecutor {
    private final MineSystemPlugin plugin;

    public JoinSphereCommand(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        int cost = 10;
        if (!plugin.getStaminaManager().hasStamina(player.getUniqueId(), cost)) {
            player.sendMessage("You are too tired to enter a sphere.");
            return true;
        }
        if (!plugin.getSphereManager().canCreateSphere(player)) {
            return true; // message handled in manager
        }
        plugin.getStaminaManager().deductStamina(player.getUniqueId(), cost);
        plugin.getSphereManager().registerSphere(player);
        player.sendMessage("Sphere created! (placeholder)");
        return true;
    }
}
