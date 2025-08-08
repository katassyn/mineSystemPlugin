package org.maks.mineSystemPlugin.repository;

import org.maks.mineSystemPlugin.database.DatabaseManager;
import org.maks.mineSystemPlugin.model.QuestData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class QuestRepository {
    private final DatabaseManager database;

    public QuestRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Optional<QuestData>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT progress FROM quests WHERE uuid = ?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new QuestData(uuid, rs.getInt("progress")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(QuestData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO quests(uuid, progress) VALUES(?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET progress=excluded.progress";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.progress());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, database.getExecutor());
    }
}
