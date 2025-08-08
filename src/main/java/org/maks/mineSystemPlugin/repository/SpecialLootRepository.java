package org.maks.mineSystemPlugin.repository;

import org.bukkit.Material;
import org.maks.mineSystemPlugin.SpecialLootEntry;
import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for persisting per-schematic loot tables used in special zones.
 */
public class SpecialLootRepository {
    private final DatabaseManager database;

    public SpecialLootRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Map<String, Map<Material, SpecialLootEntry>>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Map<Material, SpecialLootEntry>> result = new HashMap<>();
            String sql = "SELECT schematic, material, amount, chance FROM special_loot";
            try (Connection con = database.getDataSource().getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String schem = rs.getString("schematic");
                    Material mat = Material.matchMaterial(rs.getString("material"));
                    int amount = rs.getInt("amount");
                    int chance = rs.getInt("chance");
                    if (mat == null) continue;
                    result.computeIfAbsent(schem, k -> new HashMap<>())
                            .put(mat, new SpecialLootEntry(amount, chance));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return result;
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(String schematic, Map<Material, SpecialLootEntry> items) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = database.getDataSource().getConnection()) {
                try (PreparedStatement del = con.prepareStatement("DELETE FROM special_loot WHERE schematic = ?")) {
                    del.setString(1, schematic);
                    del.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO special_loot(schematic, material, amount, chance) VALUES(?, ?, ?, ?)");) {
                    for (Map.Entry<Material, SpecialLootEntry> entry : items.entrySet()) {
                        ps.setString(1, schematic);
                        ps.setString(2, entry.getKey().name());
                        ps.setInt(3, entry.getValue().amount());
                        ps.setInt(4, entry.getValue().chance());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, database.getExecutor());
    }
}
