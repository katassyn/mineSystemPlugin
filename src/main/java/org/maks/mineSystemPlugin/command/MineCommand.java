package org.maks.mineSystemPlugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.menu.MineMenu;
import org.maks.mineSystemPlugin.sphere.SphereManager;

/**
 * Opens the mine selection GUI for players.
 */
public class MineCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SphereManager sphereManager;

    public MineCommand(JavaPlugin plugin, SphereManager sphereManager) {
        this.plugin = plugin;
        this.sphereManager = sphereManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }
        if (player.getLevel() < 70) {
            player.sendMessage(ChatColor.RED + "You must be at least level 70!");
            return true;
        }

        MineMenu menu = new MineMenu(plugin, sphereManager);
        player.openInventory(menu.getInventory());
        return true;
    }
}
