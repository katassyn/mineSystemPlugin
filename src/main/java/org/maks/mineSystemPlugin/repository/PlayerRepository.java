package org.maks.mineSystemPlugin.repository;

import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.model.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {
    private final DatabaseManager database;

    public PlayerRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Optional<PlayerData>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT stamina, reset_timestamp FROM players WHERE uuid = ?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new PlayerData(uuid, rs.getInt("stamina"), rs.getLong("reset_timestamp")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO players(uuid, stamina, reset_timestamp) VALUES(?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE stamina=VALUES(stamina), reset_timestamp=VALUES(reset_timestamp)";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.stamina());
                ps.setLong(3, data.resetTimestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, database.getExecutor());
    }
}
