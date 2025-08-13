package org.maks.mineSystemPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.SpecialLootManager;
import org.maks.mineSystemPlugin.menu.SpecialLootMenu;
import org.maks.mineSystemPlugin.repository.SpecialLootRepository;

/**
 * Command to edit loot for special schematics.
 */
public class SpecialLootCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SpecialLootRepository storage;
    private final SpecialLootManager manager;

    public SpecialLootCommand(JavaPlugin plugin, SpecialLootRepository storage, SpecialLootManager manager) {
        this.plugin = plugin;
        this.storage = storage;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /specialloot <schematic>");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        String schem = args[0];
        if (!schem.endsWith(".schem")) {
            schem += ".schem";
        }
        SpecialLootMenu menu = new SpecialLootMenu(plugin, schem, storage, manager);
        player.openInventory(menu.getInventory());
        return true;
    }
}
