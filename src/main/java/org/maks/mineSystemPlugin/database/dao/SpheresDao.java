package org.maks.mineSystemPlugin.database.dao;

import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SpheresDao {
    private final DatabaseManager database;

    public SpheresDao(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<SphereData> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT type, start_time FROM spheres WHERE uuid=?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new SphereData(uuid, rs.getString("type"), rs.getLong("start_time"));
                }
                return null;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(SphereData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO spheres(uuid, type, start_time) VALUES(?,?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET type=excluded.type, start_time=excluded.start_time";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setString(2, data.type());
                ps.setLong(3, data.startTime());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public record SphereData(UUID uuid, String type, long startTime) {}
}

