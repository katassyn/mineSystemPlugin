package org.maks.mineSystemPlugin.command;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maks.mineSystemPlugin.stamina.StaminaManager;
import org.maks.mineSystemPlugin.repository.PlayerRepository;

/**
 * Debug command for checking stamina values in memory and database.
 */
public class StaminaDebugCommand implements CommandExecutor {
    private final StaminaManager staminaManager;
    private final PlayerRepository playerRepository;

    public StaminaDebugCommand(StaminaManager staminaManager, PlayerRepository playerRepository) {
        this.staminaManager = staminaManager;
        this.playerRepository = playerRepository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minesystem.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /stamindebug <player> [reload|save]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("reload")) {
                staminaManager.reloadPlayerData(uuid);
                sender.sendMessage(ChatColor.GREEN + "Reloaded stamina data from database for " + target.getName());
                return true;
            } else if (args[1].equalsIgnoreCase("save")) {
                staminaManager.saveAll();
                sender.sendMessage(ChatColor.GREEN + "Forced save of all stamina data");
                return true;
            }
        }

        // Display stamina info
        sender.sendMessage(ChatColor.YELLOW + "=== Stamina Debug Info for " + target.getName() + " ===");

        // Memory values
        int currentStamina = staminaManager.getStamina(uuid);
        int maxStamina = staminaManager.getMaxStamina(uuid);
        var timeUntilReset = staminaManager.getTimeUntilReset(uuid);

        sender.sendMessage(ChatColor.AQUA + "Memory Values:");
        sender.sendMessage(ChatColor.WHITE + "  Current: " + ChatColor.GREEN + currentStamina + "/" + maxStamina);
        sender.sendMessage(ChatColor.WHITE + "  Reset in: " + ChatColor.GREEN +
                         (timeUntilReset.isZero() ? "No timer" : timeUntilReset.toHours() + "h " +
                          timeUntilReset.minusHours(timeUntilReset.toHours()).toMinutes() + "m"));

        // Database values
        CompletableFuture.runAsync(() -> {
            playerRepository.load(uuid).thenAccept(optData -> {
                if (optData.isPresent()) {
                    var data = optData.get();
                    sender.sendMessage(ChatColor.AQUA + "Database Values:");
                    sender.sendMessage(ChatColor.WHITE + "  Stamina: " + ChatColor.GREEN + data.stamina());
                    sender.sendMessage(ChatColor.WHITE + "  Reset Timestamp: " + ChatColor.GREEN +
                                     (data.resetTimestamp() == 0 ? "None" : new java.util.Date(data.resetTimestamp())));
                } else {
                    sender.sendMessage(ChatColor.RED + "No data in database for this player!");
                }

                // Check if player exists in database
                boolean exists = playerRepository.playerExists(uuid);
                sender.sendMessage(ChatColor.WHITE + "  Exists in DB: " +
                                 (exists ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
            });
        });

        return true;
    }
}