package org.maks.mineSystemPlugin.database.dao;

import org.maks.mineSystemPlugin.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class QuestsDao {
    private final DatabaseManager database;

    public QuestsDao(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<QuestData> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT progress FROM quests WHERE uuid=?";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new QuestData(uuid, rs.getInt("progress"));
                }
                return null;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(QuestData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO quests(uuid, progress) VALUES(?,?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET progress=excluded.progress";
            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.progress());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, database.getExecutor());
    }

    public record QuestData(UUID uuid, int progress) {}
}

