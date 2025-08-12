package org.maks.mineSystemPlugin.repository;

import org.bukkit.inventory.ItemStack;
import org.maks.mineSystemPlugin.LootEntry;
import org.maks.mineSystemPlugin.ItemSerializer;
import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for persisting loot table configuration in MySQL.
 */
public class LootRepository {
    private final DatabaseManager database;

    public LootRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<List<LootEntry>> load() {
        return CompletableFuture.supplyAsync(() -> {
            List<LootEntry> list = new ArrayList<>();
            String sql = "SELECT item, chance FROM loot_items";
            try (Connection con = database.getDataSource().getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    ItemStack item = ItemSerializer.deserialize(rs.getString("item"));
                    if (item != null) {
                        list.add(new LootEntry(item, rs.getInt("chance")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(List<LootEntry> items) {
        return CompletableFuture.runAsync(() -> {
            try (Connection con = database.getDataSource().getConnection();
                 Statement st = con.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE loot_items");
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO loot_items(item, chance) VALUES(?, ?)")) {
                    for (LootEntry entry : items) {
                        ps.setString(1, ItemSerializer.serialize(entry.item()));
                        ps.setInt(2, entry.chance());
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
