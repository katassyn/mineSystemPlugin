package org.maks.mineSystemPlugin;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a single loot entry with a fixed chance in percent and full item metadata.
 */
public record SpecialLootEntry(ItemStack item, int chance) {}
