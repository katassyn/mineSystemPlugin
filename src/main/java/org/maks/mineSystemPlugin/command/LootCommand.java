package org.maks.mineSystemPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.maks.mineSystemPlugin.LootManager;
import org.maks.mineSystemPlugin.storage.MySqlStorage;

import java.sql.SQLException;

/**
 * Simple command to manage loot table.
 */
public class LootCommand implements CommandExecutor {
    private final MySqlStorage storage;
    private final LootManager lootManager;

    public LootCommand(MySqlStorage storage, LootManager lootManager) {
        this.storage = storage;
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/loot add <item> <chance>");
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            String item = args[1];
            double chance;
            try {
                chance = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("Invalid chance value");
                return true;
            }
            try {
                storage.saveItem(item, chance);
                lootManager.setProbabilities(storage.loadItems());
                sender.sendMessage("Saved " + item + " with chance " + chance);
            } catch (SQLException ex) {
                sender.sendMessage("Database error: " + ex.getMessage());
            }
            return true;
        }
        sender.sendMessage("Unknown subcommand");
        return true;
    }
}
