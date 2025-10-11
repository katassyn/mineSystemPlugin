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
import java.util.logging.Logger;

public class PlayerRepository {
    private final DatabaseManager database;
    private static final Logger logger = Logger.getLogger(PlayerRepository.class.getName());

    public PlayerRepository(DatabaseManager database) {
        this.database = database;
    }

    public CompletableFuture<Optional<PlayerData>> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT stamina, reset_timestamp FROM players WHERE uuid = ?";
            logger.info("Loading player data for UUID: " + uuid);

            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int stamina = rs.getInt("stamina");
                        long resetTimestamp = rs.getLong("reset_timestamp");

                        logger.info("Found player data for " + uuid +
                                   ": stamina=" + stamina +
                                   ", reset_timestamp=" + resetTimestamp);

                        return Optional.of(new PlayerData(uuid, stamina, resetTimestamp));
                    } else {
                        logger.info("No player data found for " + uuid);
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                logger.severe("Error loading player data for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return Optional.empty();
            }
        }, database.getExecutor());
    }

    public CompletableFuture<Void> save(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO players(uuid, stamina, reset_timestamp) VALUES(?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE stamina=VALUES(stamina), reset_timestamp=VALUES(reset_timestamp)";

            logger.info("Saving player data for " + data.uuid() +
                       ": stamina=" + data.stamina() +
                       ", reset_timestamp=" + data.resetTimestamp());

            try (Connection connection = database.getDataSource().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.stamina());
                ps.setLong(3, data.resetTimestamp());

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    logger.info("Successfully saved player data for " + data.uuid());
                } else {
                    logger.warning("No rows affected when saving player data for " + data.uuid());
                }
            } catch (SQLException e) {
                logger.severe("Error saving player data for " + data.uuid() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, database.getExecutor());
    }

    /**
     * Synchronously checks if player exists in database (for debugging)
     */
    public boolean playerExists(UUID uuid) {
        String sql = "SELECT 1 FROM players WHERE uuid = ?";
        try (Connection connection = database.getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("Error checking if player exists: " + e.getMessage());
            return false;
        }
    }
}
