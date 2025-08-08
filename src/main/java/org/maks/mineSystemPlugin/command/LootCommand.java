package org.maks.mineSystemPlugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.menu.LootEditMenu;
import org.maks.mineSystemPlugin.storage.MySqlStorage;

/**
 * Command for managing loot configuration.
 */
public class LootCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final MySqlStorage storage;
    private final LootManager lootManager;

    public LootCommand(JavaPlugin plugin, MySqlStorage storage, LootManager lootManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            LootEditMenu menu = new LootEditMenu(plugin, storage, lootManager);
            player.openInventory(menu.getInventory());
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            Inventory inv = Bukkit.createInventory(null, 27, "Loot Test");
            lootManager.fillInventory(inv);
            player.openInventory(inv);
            return true;
        }
        sender.sendMessage("/loot gui - edit loot table");
        sender.sendMessage("/loot test - preview generated loot");
        return true;
    }
}
