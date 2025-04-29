package com.example.myjavafxapp.Models.user;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserActivityLogManager {
    private static UserActivityLogManager instance;

    private UserActivityLogManager() {
        // Ensure the table exists when manager is instantiated
        createTableIfNotExists();
    }

    public static synchronized UserActivityLogManager getInstance() {
        if (instance == null) {
            instance = new UserActivityLogManager();
        }
        return instance;
    }

    /**
     * Create the user_activity_log table if it doesn't exist
     */
    private void createTableIfNotExists() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS user_activity_log (" +
                    "log_id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                    "user_id VARCHAR(50) NOT NULL, " +
                    "username VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(50), " +
                    "action_type VARCHAR(20) NOT NULL, " +
                    "timestamp DATETIME NOT NULL" +
                    ")";

            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.err.println("Error creating user_activity_log table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log a user login or logout activity
     * @param log The UserActivityLog object to be saved
     * @return true if successful, false otherwise
     */
    public boolean logActivity(UserActivityLog log) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            String insertSQL = "INSERT INTO user_activity_log " +
                    "(user_id, username, role, action_type, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, log.getUserId());
            pstmt.setString(2, log.getUsername());
            pstmt.setString(3, log.getRole());
            pstmt.setString(4, log.getActionType());
            pstmt.setTimestamp(5, Timestamp.valueOf(log.getTimestamp()));

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error logging user activity: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Log a user login
     * @param userId User's CIN/ID
     * @param username User's username
     * @param role User's role
     * @return true if successful, false otherwise
     */
    public boolean logLogin(String userId, String username, String role) {
        UserActivityLog log = new UserActivityLog(userId, username, role, "LOGIN");
        return logActivity(log);
    }

    /**
     * Log a user logout
     * @param userId User's CIN/ID
     * @param username User's username
     * @param role User's role
     * @return true if successful, false otherwise
     */
    public boolean logLogout(String userId, String username, String role) {
        UserActivityLog log = new UserActivityLog(userId, username, role, "LOGOUT");
        return logActivity(log);
    }

    /**
     * Get all activity logs
     * @return List of all activity logs
     */
    public List<UserActivityLog> getAllLogs() {
        List<UserActivityLog> logs = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM user_activity_log ORDER BY timestamp DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                UserActivityLog log = new UserActivityLog(
                        rs.getInt("log_id"),
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("action_type"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving activity logs: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    /**
     * Get activity logs for a specific user
     * @param userId User's CIN/ID
     * @return List of activity logs for the user
     */
    public List<UserActivityLog> getLogsByUser(String userId) {
        List<UserActivityLog> logs = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM user_activity_log WHERE user_id = ? ORDER BY timestamp DESC";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UserActivityLog log = new UserActivityLog(
                        rs.getInt("log_id"),
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("action_type"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user activity logs: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }
}