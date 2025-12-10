package org.maks.mineSystemPlugin.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;
import org.maks.mineSystemPlugin.MineSystemPlugin;
import org.maks.mineSystemPlugin.events.OreMinedEvent;
import org.maks.mineSystemPlugin.item.CustomItems;
import org.maks.mineSystemPlugin.tool.CustomTool;

/**
 * Handles custom mining logic. Each ore requires a configured number of hits
 * before it breaks. When the threshold is reached an item matching the
 * items.yml definition is dropped and the block is removed.
 */
public class BlockBreakListener implements Listener {

    private final MineSystemPlugin plugin;

    // Tier mapping for ores (same as in QuestSystem MineSystemListener)
    private static final java.util.Map<String, Integer> ORE_TIERS = java.util.Map.ofEntries(
            // Tier 1
            java.util.Map.entry("Hematite", 1),
            java.util.Map.entry("Magnetite", 1),
            java.util.Map.entry("Azurite", 1),
            java.util.Map.entry("Carnelian", 1),
            java.util.Map.entry("Pyrite", 1),
            java.util.Map.entry("Malachite", 1),
            java.util.Map.entry("Danburite", 1),
            // Tier 2
            java.util.Map.entry("BlackSpinel", 2),
            java.util.Map.entry("Silver", 2),
            java.util.Map.entry("Tanzanite", 2),
            java.util.Map.entry("RedSpinel", 2),
            java.util.Map.entry("YellowTopaz", 2),
            java.util.Map.entry("Peridot", 2),
            java.util.Map.entry("Goshenite", 2),
            // Tier 3
            java.util.Map.entry("BlackDiamond", 3),
            java.util.Map.entry("Osmium", 3),
            java.util.Map.entry("BlueSapphire", 3),
            java.util.Map.entry("PigeonBloodRuby", 3),
            java.util.Map.entry("YellowSapphire", 3),
            java.util.Map.entry("TropicheEmerald", 3),
            java.util.Map.entry("Cerussite", 3)
    );

    public BlockBreakListener(MineSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.isCustomOre(block.getType())) {
            return;
        }

        if (plugin.consumePlayerPlaced(block.getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        boolean bypass = player.isOp() || player.hasPermission("minesystem.admin");
        if (!bypass && !plugin.getSphereManager().isInsideSphere(block.getLocation())) {
            event.setCancelled(true);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        Material oreType = block.getType();

        String oreId = plugin.resolveOreId(block);
        var loc = block.getLocation().toBlockLocation();
        int remaining = plugin.decrementBlockHits(loc, oreId);

        plugin.getSphereManager().updateHologram(loc, oreId, remaining);

        // NOTE: OreHitEvent is no longer fired here - quest progress is tracked via OreMinedEvent
        // which fires only when the ore is fully mined and drops items (see below)
        int pickaxeLevel = CustomTool.getToolLevel(tool);

        event.setCancelled(true);

        int total = plugin.incrementOreCount(player.getUniqueId());
        if (total % 20 == 0) {
            plugin.dropRandomOreReward(player, block.getLocation());
        }

        if (remaining > 0) {
            return;
        }

        event.setDropItems(false);

        int dupLevel = CustomTool.getDuplicateLevel(tool, plugin);
        double baseChance = switch (dupLevel) {
            case 1 -> 0.05;
            case 2 -> 0.1;
            case 3 -> 0.15;
            case 4 -> 0.22;
            case 5 -> 0.30;
            default -> 0.0;
        };

        // Add pet duplication bonuses
        double petDuplicationChance = getPetOreDuplicationChance(player);
        double totalChance = baseChance + (petDuplicationChance / 100.0);

        boolean duplicate = Math.random() < totalChance;
        ItemStack drop = CustomItems.get(oreId);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            if (duplicate) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
            }
        }

        block.setType(Material.AIR);

        int amount = drop == null ? 0 : (duplicate ? drop.getAmount() * 2 : drop.getAmount());
        // pickaxeLevel already defined above for OreHitEvent
        Bukkit.getPluginManager().callEvent(new OreMinedEvent(player, oreType, amount, pickaxeLevel));

        // Fire quest events for QuestSystem integration
        if (amount > 0) {
            try {
                Class<?> listenerClass = Class.forName("org.maks.questsystem.listeners.MineSystemListener");

                // Fire OreMinedEvent (general)
                Class<?> oreMinedEventClass = null;
                for (Class<?> innerClass : listenerClass.getDeclaredClasses()) {
                    if (innerClass.getSimpleName().equals("OreMinedEvent")) {
                        oreMinedEventClass = innerClass;
                        break;
                    }
                }
                if (oreMinedEventClass != null) {
                    Object questEvent = oreMinedEventClass.getConstructor(Player.class, String.class, int.class)
                            .newInstance(player, oreId, amount);
                    Bukkit.getPluginManager().callEvent((org.bukkit.event.Event) questEvent);
                }

                // Fire OreMinedTierEvent (tier-specific)
                Class<?> oreMinedTierEventClass = null;
                for (Class<?> innerClass : listenerClass.getDeclaredClasses()) {
                    if (innerClass.getSimpleName().equals("OreMinedTierEvent")) {
                        oreMinedTierEventClass = innerClass;
                        break;
                    }
                }
                if (oreMinedTierEventClass != null) {
                    // Determine tier from ore ID using the tier mapping
                    Integer tier = ORE_TIERS.get(oreId);
                    if (tier == null) {
                        tier = 1; // Default to tier 1 if unknown ore
                    }

                    Object tierEvent = oreMinedTierEventClass.getConstructor(Player.class, int.class, int.class)
                            .newInstance(player, tier, amount);
                    Bukkit.getPluginManager().callEvent((org.bukkit.event.Event) tierEvent);
                }

                // Track pickaxe level usage
                if (pickaxeLevel > 0) {
                    Class<?> sphereCompleteEventClass = null;
                    for (Class<?> innerClass : listenerClass.getDeclaredClasses()) {
                        if (innerClass.getSimpleName().equals("SphereCompleteEvent")) {
                            sphereCompleteEventClass = innerClass;
                            break;
                        }
                    }
                    // Note: This is for pickaxe tracking, reusing sphere event structure
                    // The listener will handle pickaxe level tracking separately
                }
            } catch (Exception ignored) {
                // QuestSystem might not be loaded
            }
        }
    }

    /**
     * Get CREEPER pet ore duplication chance percentage
     */
    private double getPetOreDuplicationChance(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return 0.0;
        }

        String placeholder = PlaceholderAPI.setPlaceholders(player, "%petplugin_ore_duplication_chance%");
        try {
            return Double.parseDouble(placeholder);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
