package org.maks.mineSystemPlugin.repository;

import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.ItemSerializer;
import org.maks.mineSystemPlugin.SpecialLootEntry;
import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public CompletableFuture<Map<String, List<SpecialLootEntry>>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<SpecialLootEntry>> result = new HashMap<>();
            String sql = "SELECT schematic, item, chance FROM special_loot";
            try (Connection con = database.getDataSource().getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String schem = rs.getString("schematic");
                    ItemStack item = ItemSerializer.deserialize(rs.getString("item"));
                    int chance = rs.getInt("chance");
                    if (item == null) continue;
                    result.computeIfAbsent(schem, k -> new ArrayList<>())
                            .add(new SpecialLootEntry(item, chance));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return result;
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(String schematic, List<SpecialLootEntry> items) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = database.getDataSource().getConnection()) {
                boolean hasMaterial = false;
                try {
                    DatabaseMetaData meta = con.getMetaData();
                    try (ResultSet rs = meta.getColumns(null, null, "special_loot", "material")) {
                        hasMaterial = rs.next();
                    }
                } catch (SQLException ignored) {
                }

                try (PreparedStatement del = con.prepareStatement("DELETE FROM special_loot WHERE schematic = ?")) {
                    del.setString(1, schematic);
                    del.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO special_loot(schematic, item, chance) VALUES(?, ?, ?)")) {

                    for (SpecialLootEntry entry : items) {
                        ps.setString(1, schematic);
                        ps.setString(2, ItemSerializer.serialize(entry.item()));
                        ps.setInt(3, entry.chance());
                        if (hasMaterial) {
                            ps.setString(4, entry.item().getType().name());
                        }
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
