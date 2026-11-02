package dao;

import db.DBConnection;
import model.AdminUser;
import util.SecurityUtil;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Admin user operations.
 * Supports MySQL JDBC and automatic offline/demo fallback when MySQL is not running.
 */
public class AdminDAO {

    // Fallback store for offline/demo mode
    private static final ConcurrentHashMap<String, String> FALLBACK_ADMINS = new ConcurrentHashMap<>();

    static {
        // admin / admin123
        FALLBACK_ADMINS.put("admin", "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9");
    }

    public boolean authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) return false;

        String hash = SecurityUtil.sha256(password);
        String sql = "SELECT admin_id, username FROM admin_users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ps.setString(2, hash);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // If MySQL is offline, verify against demo store
            String storedHash = FALLBACK_ADMINS.get(username.trim());
            if (storedHash != null && storedHash.equalsIgnoreCase(hash)) {
                return true;
            }
            // If it failed because credentials are wrong, return false
            if (FALLBACK_ADMINS.containsKey(username.trim())) {
                return false;
            }
            // If MySQL error is genuine and not in fallback
            throw e;
        }
    }

    public boolean hasAdminUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admin_users";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return !FALLBACK_ADMINS.isEmpty();
        }
    }

    public boolean registerAdmin(String username, String rawPassword) throws SQLException {
        String hash = SecurityUtil.sha256(rawPassword);
        FALLBACK_ADMINS.put(username.trim(), hash);

        String sql = "INSERT INTO admin_users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ps.setString(2, hash);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Already saved in fallback
            return true;
        }
    }
}
