package com.example.myjavafxapp.Models.backup;

import java.time.LocalTime;

public class BackupSchedule {
    private int scheduleID;
    private boolean enabled;
    private LocalTime backupTime;
    private String backupFormat; // "CSV" or "SQL"
    private String backupLocation;
    private boolean backupOnExit;
    private boolean backupOnStartup;
    private int retentionDays; // How many days to keep backups

    // Constructor
    public BackupSchedule() {
        // Default values
        this.enabled = false;
        this.backupTime = LocalTime.of(23, 0); // 11:00 PM by default
        this.backupFormat = "SQL";
        this.backupLocation = System.getProperty("user.home") + "/heartcare/backups";
        this.backupOnExit = false;
        this.backupOnStartup = false;
        this.retentionDays = 30;
    }

    // All args constructor
    public BackupSchedule(int scheduleID, boolean enabled, LocalTime backupTime,
                          String backupFormat, String backupLocation,
                          boolean backupOnExit, boolean backupOnStartup, int retentionDays) {
        this.scheduleID = scheduleID;
        this.enabled = enabled;
        this.backupTime = backupTime;
        this.backupFormat = backupFormat;
        this.backupLocation = backupLocation;
        this.backupOnExit = backupOnExit;
        this.backupOnStartup = backupOnStartup;
        this.retentionDays = retentionDays;
    }

    // Getters and setters
    public int getScheduleID() {
        return scheduleID;
    }

    public void setScheduleID(int scheduleID) {
        this.scheduleID = scheduleID;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(LocalTime backupTime) {
        this.backupTime = backupTime;
    }

    public String getBackupFormat() {
        return backupFormat;
    }

    public void setBackupFormat(String backupFormat) {
        this.backupFormat = backupFormat;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }

    public boolean isBackupOnExit() {
        return backupOnExit;
    }

    public void setBackupOnExit(boolean backupOnExit) {
        this.backupOnExit = backupOnExit;
    }

    public boolean isBackupOnStartup() {
        return backupOnStartup;
    }

    public void setBackupOnStartup(boolean backupOnStartup) {
        this.backupOnStartup = backupOnStartup;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}