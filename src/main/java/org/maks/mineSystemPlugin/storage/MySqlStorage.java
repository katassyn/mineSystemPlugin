package org.maks.mineSystemPlugin.storage;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple JDBC backed storage for loot configuration.
 * Configuration is stored in a table named <code>loot_items</code>
 * where each row contains the MythicMobs item name and the chance in percent.
 */
public class MySqlStorage {
    private final Connection connection;

    public MySqlStorage(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS loot_items (item VARCHAR(64) PRIMARY KEY, chance DOUBLE)");
        }
    }

    public void saveItem(String item, double chance) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO loot_items(item, chance) VALUES(?, ?)")) {
            ps.setString(1, item);
            ps.setDouble(2, chance);
            ps.executeUpdate();
        }
    }

    public Map<String, Double> loadItems() throws SQLException {
        Map<String, Double> map = new HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT item, chance FROM loot_items")) {
            while (rs.next()) {
                map.put(rs.getString("item"), rs.getDouble("chance"));
            }
        }
        return map;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
