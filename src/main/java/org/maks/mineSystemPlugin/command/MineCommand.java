package org.maks.mineSystemPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.menu.MineMenu;

/**
 * Opens the mine selection GUI for players.
 */
public class MineCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public MineCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }
        MineMenu menu = new MineMenu(plugin);
        player.openInventory(menu.getInventory());
        return true;
    }
}
