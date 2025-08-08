package org.maks.mineSystemPlugin.repository;

import org.bukkit.Material;
import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for persisting loot table configuration in MySQL.
 */
public class LootRepository {
    private final DatabaseManager database;

    public LootRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Map<Material, Integer>> load() {
        return CompletableFuture.supplyAsync(() -> {
            Map<Material, Integer> map = new HashMap<>();
            String sql = "SELECT material, chance FROM loot_items";
            try (Connection con = database.getDataSource().getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Material mat = Material.matchMaterial(rs.getString("material"));
                    if (mat != null) {
                        map.put(mat, rs.getInt("chance"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(Map<Material, Integer> items) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = database.getDataSource().getConnection();
                 Statement st = con.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE loot_items");
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO loot_items(material, chance) VALUES(?, ?)")) {
                    for (Map.Entry<Material, Integer> entry : items.entrySet()) {
                        ps.setString(1, entry.getKey().name());
                        ps.setInt(2, entry.getValue());
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
