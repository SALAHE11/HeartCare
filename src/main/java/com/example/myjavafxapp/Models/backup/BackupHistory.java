package com.example.myjavafxapp.Models.backup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupHistory {
    private int backupID;
    private LocalDateTime backupDateTime;
    private String backupType; // "Manual" or "Automatic"
    private String backupFormat; // "CSV" or "SQL"
    private String backupPath;
    private long backupSize; // Size in bytes
    private String backupStatus; // "Success", "Failed", "In Progress"
    private String backupDescription;
    private String createdBy;

    // Constructors
    public BackupHistory() {
        // Default constructor
    }

    public BackupHistory(int backupID, LocalDateTime backupDateTime, String backupType,
                         String backupFormat, String backupPath, long backupSize,
                         String backupStatus, String backupDescription, String createdBy) {
        this.backupID = backupID;
        this.backupDateTime = backupDateTime;
        this.backupType = backupType;
        this.backupFormat = backupFormat;
        this.backupPath = backupPath;
        this.backupSize = backupSize;
        this.backupStatus = backupStatus;
        this.backupDescription = backupDescription;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public int getBackupID() {
        return backupID;
    }

    public void setBackupID(int backupID) {
        this.backupID = backupID;
    }

    public LocalDateTime getBackupDateTime() {
        return backupDateTime;
    }

    public void setBackupDateTime(LocalDateTime backupDateTime) {
        this.backupDateTime = backupDateTime;
    }

    public String getBackupType() {
        return backupType;
    }

    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }

    public String getBackupFormat() {
        return backupFormat;
    }

    public void setBackupFormat(String backupFormat) {
        this.backupFormat = backupFormat;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public long getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(long backupSize) {
        this.backupSize = backupSize;
    }

    public String getBackupStatus() {
        return backupStatus;
    }

    public void setBackupStatus(String backupStatus) {
        this.backupStatus = backupStatus;
    }

    public String getBackupDescription() {
        return backupDescription;
    }

    public void setBackupDescription(String backupDescription) {
        this.backupDescription = backupDescription;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Convenience methods for UI display
    public String getFormattedDateTime() {
        if (backupDateTime == null) return "";
        return backupDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getFormattedSize() {
        if (backupSize < 1024) return backupSize + " B";
        if (backupSize < 1024 * 1024) return String.format("%.2f KB", backupSize / 1024.0);
        if (backupSize < 1024 * 1024 * 1024) return String.format("%.2f MB", backupSize / (1024.0 * 1024.0));
        return String.format("%.2f GB", backupSize / (1024.0 * 1024.0 * 1024.0));
    }
}