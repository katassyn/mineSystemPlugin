package org.maks.mineSystemPlugin.command;

import java.time.Duration;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maks.mineSystemPlugin.stamina.StaminaManager;

/**
 * Displays a player's current stamina and time until it resets.
 */
public class StaminaCommand implements CommandExecutor {
    private final StaminaManager staminaManager;

    public StaminaCommand(StaminaManager staminaManager) {
        this.staminaManager = staminaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command.");
            return true;
        }
        UUID uuid = player.getUniqueId();
        int stamina = staminaManager.getStamina(uuid);
        int max = staminaManager.getMaxStamina(uuid);
        Duration remaining = staminaManager.getTimeUntilReset(uuid);
        long minutes = remaining.toMinutes();
        long seconds = remaining.minusMinutes(minutes).getSeconds();
        player.sendMessage(ChatColor.YELLOW + "Stamina: " + stamina + "/" + max);
        if (!remaining.isZero()) {
            player.sendMessage(ChatColor.YELLOW + "Resets in: " + minutes + "m " + seconds + "s");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Stamina is full.");
        }
        return true;
    }
}
