package org.maks.mineSystemPlugin.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides ItemStack templates matching entries from items.yml so the plugin
 * can dispense custom items without relying on MythicMobs.
 */
public final class CustomItems {

    private static final Map<String, ItemStack> ITEMS = new HashMap<>();
    private static final Map<String, String> NAME_TO_ID = new HashMap<>();

    static {
        // Black Ore (Coal-based)
        register("Hematite", Material.COAL_ORE, "&8Hematite",
                List.of("&7A common black ore with metallic luster and reddish streaks.",
                        "&eCoal-based mineral"), true, true);
        register("BlackSpinel", Material.COAL_ORE, "&8Black Spinel",
                List.of("&7An uncommon black crystal with exceptional hardness and luster.",
                        "&eCoal-based gemstone"), true, true);
        register("BlackDiamond", Material.COAL_ORE, "&8&lBlack Diamond",
                List.of("&7A rare and precious black diamond formed under extreme pressure.",
                        "&eHighest quality coal gem"), true, true);

        // Metallic Ore (Iron-based)
        register("Magnetite", Material.IRON_ORE, "&7Magnetite",
                List.of("&7A common metallic ore with magnetic properties.",
                        "&eIron-based mineral"), true, true);
        register("Silver", Material.IRON_ORE, "&f&lSilver",
                List.of("&7An uncommon precious metal with lustrous white appearance.",
                        "&eIron-based metal"), true, true);
        register("Osmium", Material.IRON_ORE, "&7&lOsmium",
                List.of("&7A rare and dense bluish-white metal, one of the heaviest natural elements.",
                        "&ePremium iron metal"), true, true);

        // Azure Ore (Lapis-based)
        register("Azurite", Material.LAPIS_ORE, "&9Azurite",
                List.of("&7A common deep blue mineral with intense azure color.",
                        "&eLapis-based mineral"), true, true);
        register("Tanzanite", Material.LAPIS_ORE, "&9&lTanzanite",
                List.of("&7An uncommon blue-purple gemstone known for its trichroic properties.",
                        "&eLapis-based gem"), true, true);
        register("BlueSapphire", Material.LAPIS_ORE, "&1&lBlue Sapphire",
                List.of("&7A rare and precious blue gemstone, second only to diamond in hardness.",
                        "&ePremium lapis gem"), true, true);

        // Crimson Ore (Redstone-based)
        register("Carnelian", Material.REDSTONE_ORE, "&cCarnelian",
                List.of("&7A common reddish-orange mineral with translucent properties.",
                        "&eRedstone-based mineral"), true, true);
        register("RedSpinel", Material.REDSTONE_ORE, "&c&lRed Spinel",
                List.of("&7An uncommon vibrant red gemstone often mistaken for ruby.",
                        "&eRedstone-based gem"), true, true);
        register("PigeonBloodRuby", Material.REDSTONE_ORE, "&4&lPigeon Blood Ruby",
                List.of("&7A rare and precious deep red gemstone with the coveted \"pigeon blood\" color.",
                        "&ePremium redstone gem"), true, true);

        // Golden Ore (Gold-based)
        register("Pyrite", Material.GOLD_ORE, "&ePyrite",
                List.of("&7A common brassy-yellow mineral often called \"Fool`s Gold\".",
                        "&eGold-based mineral"), true, true);
        register("YellowTopaz", Material.GOLD_ORE, "&e&lYellow Topaz",
                List.of("&7An uncommon golden-yellow gemstone with excellent clarity.",
                        "&eGold-based gem"), true, true);
        register("YellowSapphire", Material.GOLD_ORE, "&6&lYellow Sapphire",
                List.of("&7A rare and precious golden gemstone, prized for its brilliance and hardness.",
                        "&ePremium gold gem"), true, true);

        // Verdant Ore (Emerald-based)
        register("Malachite", Material.EMERALD_ORE, "&aMalachite",
                List.of("&7A common green mineral with distinctive banded patterns.",
                        "&eEmerald-based mineral"), true, true);
        register("Peridot", Material.EMERALD_ORE, "&a&lPeridot",
                List.of("&7An uncommon olive-green gemstone formed in volcanic environments.",
                        "&eEmerald-based gem"), true, true);
        register("TropicheEmerald", Material.EMERALD_ORE, "&2&lTropiche Emerald",
                List.of("&7A rare and precious deep green gemstone with exceptional clarity and color.",
                        "&ePremium emerald"), true, true);

        // Prismatic Ore (Diamond-based)
        register("Danburite", Material.DIAMOND_ORE, "&fDanburite",
                List.of("&7A common colorless crystal with diamond-like brilliance.",
                        "&eDiamond-based mineral"), true, true);
        register("Goshenite", Material.DIAMOND_ORE, "&f&lGoshenite",
                List.of("&7An uncommon colorless beryl with exceptional clarity.",
                        "&eDiamond-based gemstone"), true, true);
        register("Cerussite", Material.DIAMOND_ORE, "&f&l&nCerussite",
                List.of("&7A rare and precious crystal with the highest refractive index.",
                        "&eSupreme diamond gemstone"), true, true);

        // Bonus ore items
        register("ore_I", Material.COBBLESTONE, "&9[ I ] Ore",
                List.of("§o&7Basic crafting material"), true, true);
        register("ore_II", Material.COBBLESTONE, "&5[ II ] Ore",
                List.of("§o&7Basic crafting material"), true, true);
        register("ore_III", Material.COBBLESTONE, "&6[ III ] Ore",
                List.of("§o&7Basic crafting material"), true, true);

        // Leaf items
        register("leaf_I", Material.KELP, "&9[ I ] Leaf",
                List.of("§o&7Basic crafting material"), true, true);
        register("leaf_II", Material.KELP, "&5[ II ] Leaf",
                List.of("§o&7Basic crafting material"), true, true);
        register("leaf_III", Material.KELP, "&6[ III ] Leaf",
                List.of("§o&7Basic crafting material"), true, true);

        // Bone items
        register("bone_I", Material.BONE, "&9[ I ] Shattered Bone",
                List.of("§o&aRare crafting material"), true, true);
        register("bone_II", Material.BONE, "&5[ II ] Shattered Bone",
                List.of("§o&aRare crafting material"), true, true);
        register("bone_III", Material.BONE, "&6[ III ] Shattered Bone",
                List.of("§o&aRare crafting material"), true, true);

        // Currency
        register("Crystal", Material.BRICK, "&d&lCrystal",
                List.of("§o&7Mine currency"), true, true);

        // Consumables
        register("miner_elixir", Material.RAW_BEEF, "&5Miner Elixir",
                List.of("&7Refresh Mining Stamin"), true, true);
    }

    private static void register(String id, Material material, String name,
                                 List<String> lore, boolean enchant, boolean unbreakable) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (name != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_POTION_EFFECTS,
                ItemFlag.HIDE_PLACED_ON);
        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        if (enchant) {
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        stack.setItemMeta(meta);
        ITEMS.put(id, stack);
        if (meta.hasDisplayName()) {
            NAME_TO_ID.put(meta.getDisplayName(), id);
        }
    }

    /**
     * Returns a cloned instance of the configured item or null if the id is
     * unknown.
     */
    public static ItemStack get(String id) {
        ItemStack stack = ITEMS.get(id);
        return stack != null ? stack.clone() : null;
    }

    /**
     * Attempts to resolve the registered id for a given item instance.
     */
    public static String getId(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return NAME_TO_ID.get(meta.getDisplayName());
        }
        return null;
    }

    private CustomItems() {
    }
}

