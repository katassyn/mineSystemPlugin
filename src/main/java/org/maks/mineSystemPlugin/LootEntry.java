package org.maks.mineSystemPlugin;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a loot item with its chance in percent.
 */
public record LootEntry(ItemStack item, int chance) {}
