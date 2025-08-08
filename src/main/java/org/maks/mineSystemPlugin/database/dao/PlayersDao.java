package org.maks.mineSystemPlugin.database.dao;

import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PlayersDao {
    private final DatabaseManager database;

    public PlayersDao(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<PlayerData> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT stamina, reset_timestamp FROM players WHERE uuid=?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PlayerData(uuid, rs.getInt("stamina"), rs.getLong("reset_timestamp"));
                }
                return null;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO players(uuid, stamina, reset_timestamp) VALUES(?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET stamina=excluded.stamina, reset_timestamp=excluded.reset_timestamp";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.stamina());
                ps.setLong(3, data.resetTimestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public record PlayerData(UUID uuid, int stamina, long resetTimestamp) {}
}

