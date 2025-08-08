package org.maks.mineSystemPlugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private final HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public DatabaseManager(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "data.db"));
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        initTables();
    }

    private void initTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, stamina INTEGER, reset_timestamp INTEGER)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pickaxes (uuid TEXT PRIMARY KEY, material TEXT, durability INTEGER, enchants TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quests (uuid TEXT PRIMARY KEY, progress INTEGER)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS spheres (uuid TEXT PRIMARY KEY, type TEXT, start_time INTEGER)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void close() {
        dataSource.close();
        executor.shutdownNow();
    }
}

