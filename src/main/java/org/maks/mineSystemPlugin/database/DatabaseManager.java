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
        config.setMaximumPoolSize(20); // Increased for 30+ players - mining system operations
        config.setMinimumIdle(8); // Lower priority plugin - basic connections
        config.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(config);
        initTables();
    }

    private void initTables() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, stamina INT, reset_timestamp BIGINT)");
            // ensure newly added columns exist on old installations
            try {
                statement.executeUpdate("ALTER TABLE players ADD COLUMN stamina INT DEFAULT 100");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                statement.executeUpdate("ALTER TABLE players ADD COLUMN reset_timestamp BIGINT DEFAULT 0");
            } catch (SQLException ignore) {
                // column already exists
            }
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pickaxes (uuid VARCHAR(36) PRIMARY KEY, material VARCHAR(32), durability INT, enchants TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quests (uuid VARCHAR(36) PRIMARY KEY, progress INT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS spheres (uuid VARCHAR(36) PRIMARY KEY, type VARCHAR(32), start_time BIGINT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS loot_items (id INT AUTO_INCREMENT PRIMARY KEY, item TEXT, chance INT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS special_loot (id INT AUTO_INCREMENT PRIMARY KEY, schematic VARCHAR(64), item TEXT, chance INT)");
            try {
                statement.executeUpdate("ALTER TABLE loot_items ADD COLUMN item TEXT");
            } catch (SQLException ignore) {
            }
            try {
                statement.executeUpdate("ALTER TABLE loot_items ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY");
            } catch (SQLException ignore) {
            }
            try {
                statement.executeUpdate("ALTER TABLE loot_items DROP COLUMN material");
            } catch (SQLException ignore) {
            }
            try {
                statement.executeUpdate("ALTER TABLE special_loot ADD COLUMN item TEXT");
            } catch (SQLException ignore) {
            }
            try {
                statement.executeUpdate("ALTER TABLE special_loot ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY");
            } catch (SQLException ignore) {
            }
            try {
                statement.executeUpdate("ALTER TABLE special_loot DROP COLUMN material");
            } catch (SQLException ignore) {
            }
            try {
                statement.executeUpdate("ALTER TABLE special_loot DROP COLUMN amount");
            } catch (SQLException ignore) {
            }
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

