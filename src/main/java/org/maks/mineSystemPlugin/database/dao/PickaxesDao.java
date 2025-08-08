package org.maks.mineSystemPlugin.database.dao;

import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PickaxesDao {
    private final DatabaseManager database;

    public PickaxesDao(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<PickaxeData> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT material, durability, enchants FROM pickaxes WHERE uuid=?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PickaxeData(uuid, rs.getString("material"), rs.getInt("durability"), rs.getString("enchants"));
                }
                return null;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(PickaxeData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO pickaxes(uuid, material, durability, enchants) VALUES(?,?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET material=excluded.material, durability=excluded.durability, enchants=excluded.enchants";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setString(2, data.material());
                ps.setInt(3, data.durability());
                ps.setString(4, data.enchants());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public record PickaxeData(UUID uuid, String material, int durability, String enchants) {}
}

