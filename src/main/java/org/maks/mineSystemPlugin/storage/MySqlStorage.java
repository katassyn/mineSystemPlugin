package org.maks.mineSystemPlugin.storage;

import org.bukkit.Material;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple JDBC backed storage for loot configuration.
 * Configuration is stored in a table named <code>loot_items</code>
 * where each row contains the item material and the chance in percent.
 */
public class MySqlStorage {
    private final Connection connection;

    public MySqlStorage(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS loot_items (material VARCHAR(64) PRIMARY KEY, chance INT)");
        }
    }

    /**
     * Replaces all stored loot items with provided map.
     */
    public void saveItems(Map<Material, Integer> items) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE loot_items");
        }
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO loot_items(material, chance) VALUES(?, ?)");) {
            for (Map.Entry<Material, Integer> entry : items.entrySet()) {
                ps.setString(1, entry.getKey().name());
                ps.setInt(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Map<Material, Integer> loadItems() throws SQLException {
        Map<Material, Integer> map = new HashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT material, chance FROM loot_items")) {
            while (rs.next()) {
                Material mat = Material.matchMaterial(rs.getString("material"));
                if (mat != null) {
                    map.put(mat, rs.getInt("chance"));
                }
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
