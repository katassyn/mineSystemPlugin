package org.maks.mineSystemPlugin.repository;

import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.model.SphereData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SphereRepository {
    private final DatabaseManager database;

    public SphereRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Optional<SphereData>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT type, start_time FROM spheres WHERE uuid = ?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new SphereData(uuid, rs.getString("type"), rs.getLong("start_time")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(SphereData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO spheres(uuid, type, start_time) VALUES(?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE type=VALUES(type), start_time=VALUES(start_time)";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setString(2, data.type());
                ps.setLong(3, data.startTime());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, database.getExecutor());
    }
}
