package org.maks.mineSystemPlugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private final HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public DatabaseManager(JavaPlugin plugin) {
        var cfg = plugin.getConfig().getConfigurationSection("database");
        HikariConfig config = new HikariConfig();
        String host = cfg.getString("host", "localhost");
        int port = cfg.getInt("port", 3306);
        String db = cfg.getString("name", "minesystem");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db);
        config.setUsername(cfg.getString("user", "root"));
        config.setPassword(cfg.getString("password", ""));
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);
        initTables();
    }

    private void initTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, stamina INT, reset_timestamp BIGINT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pickaxes (uuid VARCHAR(36) PRIMARY KEY, material VARCHAR(32), durability INT, enchants TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quests (uuid VARCHAR(36) PRIMARY KEY, progress INT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS spheres (uuid VARCHAR(36) PRIMARY KEY, type VARCHAR(32), start_time BIGINT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS loot_items (material VARCHAR(64) PRIMARY KEY, chance INT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS special_loot (schematic VARCHAR(64), material VARCHAR(64), amount INT, PRIMARY KEY(schematic, material))");
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

