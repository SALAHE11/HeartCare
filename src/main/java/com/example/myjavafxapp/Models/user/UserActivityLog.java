package com.example.myjavafxapp.Models.user;

import java.time.LocalDateTime;

public class UserActivityLog {
    private int logId;
    private String userId;
    private String username;
    private String role;
    private String actionType; // "LOGIN" or "LOGOUT"
    private LocalDateTime timestamp;

    // Default constructor
    public UserActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with all fields except logId (for new entries)
    public UserActivityLog(String userId, String username, String role, String actionType) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.actionType = actionType;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with all fields
    public UserActivityLog(int logId, String userId, String username, String role,
                           String actionType, LocalDateTime timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.actionType = actionType;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserActivityLog{" +
                "logId=" + logId +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", actionType='" + actionType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}