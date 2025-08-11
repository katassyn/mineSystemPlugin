package org.maks.mineSystemPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.maks.mineSystemPlugin.item.CustomItems;

/**
 * Utility class for handling Crystal currency stored in both player inventory
 * and Ingredient Pouch.
 */
public final class CrystalCurrency {
    private static final String ITEM_ID = "Crystal";
    private static final String POUCH_ID = "crystal";

    private static Object pouchAPI;
    private static boolean apiAvailable = false;

    static {
        try {
            Object pouchPlugin = Bukkit.getPluginManager().getPlugin("IngredientPouchPlugin");
            if (pouchPlugin != null) {
                pouchAPI = pouchPlugin.getClass().getMethod("getAPI").invoke(pouchPlugin);
                apiAvailable = true;
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isCrystal(ItemStack stack) {
        return stack != null && ITEM_ID.equals(CustomItems.getId(stack));
    }

    public static int countInventory(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCrystal(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static int countPouch(Player player) {
        if (!apiAvailable) {
            return 0;
        }
        try {
            Object quantity = pouchAPI.getClass()
                    .getMethod("getItemQuantity", String.class, String.class)
                    .invoke(pouchAPI, player.getUniqueId().toString(), POUCH_ID);
            return (Integer) quantity;
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean hasCrystals(Player player, int amount) {
        return countInventory(player) + countPouch(player) >= amount;
    }

    public static boolean removeCrystals(Player player, int amount) {
        if (!hasCrystals(player, amount)) {
            return false;
        }
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (isCrystal(stack)) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setContents(contents);

        if (remaining > 0 && apiAvailable) {
            try {
                Object success = pouchAPI.getClass()
                        .getMethod("updateItemQuantity", String.class, String.class, int.class)
                        .invoke(pouchAPI, player.getUniqueId().toString(), POUCH_ID, -remaining);
                if (!(Boolean) success) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private CrystalCurrency() {}
}
