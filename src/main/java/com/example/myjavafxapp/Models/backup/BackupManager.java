package com.example.myjavafxapp.Models.backup;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.user.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class BackupManager {
    private static BackupManager instance;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledBackup;
    private BackupSchedule backupSchedule;

    // Constants
    public static final String BACKUP_TYPE_MANUAL = "Manual";
    public static final String BACKUP_TYPE_AUTOMATIC = "Automatic";

    public static final String BACKUP_FORMAT_CSV = "CSV";
    public static final String BACKUP_FORMAT_SQL = "SQL";

    public static final String BACKUP_STATUS_SUCCESS = "Success";
    public static final String BACKUP_STATUS_FAILED = "Failed";
    public static final String BACKUP_STATUS_IN_PROGRESS = "In Progress";

    // Tables to backup
    private static final String[] TABLES = {
            "patient", "users", "rendezvous", "paiment", "dossierpatient",
            "rendezvous_history", "paiment_history", "backup_history", "backup_schedule"
    };

    private BackupManager() {
        scheduler = Executors.newScheduledThreadPool(1);
        loadBackupSchedule();
    }

    public static synchronized BackupManager getInstance() {
        if (instance == null) {
            instance = new BackupManager();
        }
        return instance;
    }

    /**
     * Create the backup tables if they don't exist
     */
    public void initBackupTables() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Create backup_history table
            String createHistoryTable =
                    "CREATE TABLE IF NOT EXISTS backup_history (" +
                            "BackupID INT AUTO_INCREMENT PRIMARY KEY, " +
                            "BackupDateTime DATETIME NOT NULL, " +
                            "BackupType VARCHAR(50) NOT NULL, " +
                            "BackupFormat VARCHAR(10) NOT NULL, " +
                            "BackupPath VARCHAR(255) NOT NULL, " +
                            "BackupSize BIGINT, " +
                            "BackupStatus VARCHAR(50) NOT NULL, " +
                            "BackupDescription TEXT, " +
                            "CreatedBy VARCHAR(50)" +
                            ")";

            Statement stmt = conn.createStatement();
            stmt.execute(createHistoryTable);

            // Create backup_schedule table
            String createScheduleTable =
                    "CREATE TABLE IF NOT EXISTS backup_schedule (" +
                            "ScheduleID INT AUTO_INCREMENT PRIMARY KEY, " +
                            "Enabled TINYINT(1) NOT NULL DEFAULT 0, " +
                            "BackupTime TIME NOT NULL DEFAULT '23:00:00', " +
                            "BackupFormat VARCHAR(10) NOT NULL DEFAULT 'SQL', " +
                            "BackupLocation VARCHAR(255), " +
                            "BackupOnExit TINYINT(1) NOT NULL DEFAULT 0, " +
                            "BackupOnStartup TINYINT(1) NOT NULL DEFAULT 0, " +
                            "RetentionDays INT NOT NULL DEFAULT 30" +
                            ")";

            stmt.execute(createScheduleTable);

            // Check if we need to insert default schedule
            String checkSchedule = "SELECT COUNT(*) FROM backup_schedule";
            ResultSet rs = stmt.executeQuery(checkSchedule);

            if (rs.next() && rs.getInt(1) == 0) {
                // Insert default schedule
                String insertDefault =
                        "INSERT INTO backup_schedule (Enabled, BackupTime, BackupFormat, " +
                                "BackupLocation, BackupOnExit, BackupOnStartup, RetentionDays) " +
                                "VALUES (0, '23:00:00', 'SQL', ?, 0, 0, 30)";

                PreparedStatement pstmt = conn.prepareStatement(insertDefault);
                pstmt.setString(1, System.getProperty("user.home") + "/heartcare/backups");
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the backup schedule from the database
     */
    public void loadBackupSchedule() {
        // Make sure backup tables exist
        initBackupTables();

        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM backup_schedule ORDER BY ScheduleID LIMIT 1";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                backupSchedule = new BackupSchedule(
                        rs.getInt("ScheduleID"),
                        rs.getBoolean("Enabled"),
                        rs.getTime("BackupTime").toLocalTime(),
                        rs.getString("BackupFormat"),
                        rs.getString("BackupLocation"),
                        rs.getBoolean("BackupOnExit"),
                        rs.getBoolean("BackupOnStartup"),
                        rs.getInt("RetentionDays")
                );
            } else {
                // Create default schedule
                backupSchedule = new BackupSchedule();
            }

            // If enabled, schedule the next backup
            if (backupSchedule.isEnabled()) {
                scheduleBackup();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            backupSchedule = new BackupSchedule();
        }
    }

    /**
     * Save the backup schedule to the database
     */
    public boolean saveBackupSchedule(BackupSchedule schedule) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // If we have a schedule ID, update it
            if (schedule.getScheduleID() > 0) {
                String updateQuery =
                        "UPDATE backup_schedule SET " +
                                "Enabled = ?, BackupTime = ?, BackupFormat = ?, " +
                                "BackupLocation = ?, BackupOnExit = ?, " +
                                "BackupOnStartup = ?, RetentionDays = ? " +
                                "WHERE ScheduleID = ?";

                PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                pstmt.setBoolean(1, schedule.isEnabled());
                pstmt.setTime(2, Time.valueOf(schedule.getBackupTime()));
                pstmt.setString(3, schedule.getBackupFormat());
                pstmt.setString(4, schedule.getBackupLocation());
                pstmt.setBoolean(5, schedule.isBackupOnExit());
                pstmt.setBoolean(6, schedule.isBackupOnStartup());
                pstmt.setInt(7, schedule.getRetentionDays());
                pstmt.setInt(8, schedule.getScheduleID());

                int result = pstmt.executeUpdate();

                if (result > 0) {
                    this.backupSchedule = schedule;

                    // Update schedule
                    if (scheduledBackup != null) {
                        scheduledBackup.cancel(false);
                    }

                    if (schedule.isEnabled()) {
                        scheduleBackup();
                    }

                    return true;
                }
            } else {
                // Insert a new schedule
                String insertQuery =
                        "INSERT INTO backup_schedule (Enabled, BackupTime, BackupFormat, " +
                                "BackupLocation, BackupOnExit, BackupOnStartup, RetentionDays) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                pstmt.setBoolean(1, schedule.isEnabled());
                pstmt.setTime(2, Time.valueOf(schedule.getBackupTime()));
                pstmt.setString(3, schedule.getBackupFormat());
                pstmt.setString(4, schedule.getBackupLocation());
                pstmt.setBoolean(5, schedule.isBackupOnExit());
                pstmt.setBoolean(6, schedule.isBackupOnStartup());
                pstmt.setInt(7, schedule.getRetentionDays());

                int result = pstmt.executeUpdate();

                if (result > 0) {
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        schedule.setScheduleID(generatedKeys.getInt(1));
                    }

                    this.backupSchedule = schedule;

                    // Schedule if enabled
                    if (schedule.isEnabled()) {
                        scheduleBackup();
                    }

                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get the current backup schedule
     */
    public BackupSchedule getBackupSchedule() {
        return backupSchedule;
    }

    /**
     * Schedule the automatic backup based on the current schedule
     */
    private void scheduleBackup() {
        if (backupSchedule == null || !backupSchedule.isEnabled()) {
            return;
        }

        // Cancel any existing scheduled backup
        if (scheduledBackup != null) {
            scheduledBackup.cancel(false);
        }

        // Calculate delay until next backup
        LocalTime now = LocalTime.now();
        LocalTime backupTime = backupSchedule.getBackupTime();

        long delayInSeconds;

        if (now.isBefore(backupTime)) {
            // Later today
            delayInSeconds = Duration.between(now, backupTime).getSeconds();
        } else {
            // Tomorrow
            delayInSeconds = Duration.between(now, backupTime.plusHours(24)).getSeconds();
        }

        // Schedule the backup
        scheduledBackup = scheduler.scheduleAtFixedRate(
                () -> performBackup(BACKUP_TYPE_AUTOMATIC, backupSchedule.getBackupFormat()),
                delayInSeconds,
                24 * 60 * 60, // 24 hours
                TimeUnit.SECONDS
        );
    }

    /**
     * Perform a backup operation in a separate thread
     */
    public Task<BackupHistory> startBackupTask(String backupType, String backupFormat) {
        Task<BackupHistory> task = new Task<>() {
            @Override
            protected BackupHistory call() throws Exception {
                return performBackup(backupType, backupFormat);
            }
        };

        new Thread(task).start();
        return task;
    }

    /**
     * Perform a backup operation
     */
    public BackupHistory performBackup(String backupType, String backupFormat) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        String backupFileName = "heartcare_backup_" + timestamp;
        String extension = BACKUP_FORMAT_SQL.equals(backupFormat) ? ".sql" : ".zip";

        String backupPath = backupSchedule.getBackupLocation();
        File backupDir = new File(backupPath);

        // Create directory if it doesn't exist
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        File backupFile = new File(backupDir, backupFileName + extension);

        // Create a backup history record
        BackupHistory backup = new BackupHistory();
        backup.setBackupDateTime(now);
        backup.setBackupType(backupType);
        backup.setBackupFormat(backupFormat);
        backup.setBackupPath(backupFile.getAbsolutePath());
        backup.setBackupStatus(BACKUP_STATUS_IN_PROGRESS);

        // Get the current username or use 'System' for automatic backups
        String username = BACKUP_TYPE_AUTOMATIC.equals(backupType) ?
                "System" : UserSession.getInstance().getUsername();

        backup.setCreatedBy(username);

        // Save the initial record to get an ID
        saveBackupHistory(backup);

        try {
            if (BACKUP_FORMAT_SQL.equals(backupFormat)) {
                // SQL backup - use Java-based method instead of mysqldump
                performSqlBackup(backupFile);
            } else {
                // CSV backup
                performCsvBackup(backupDir, backupFileName);

                // Create a zip file from the CSV directory
                File csvDir = new File(backupDir, backupFileName);
                zipDirectory(csvDir, backupFile);

                // Remove the temporary CSV directory
                deleteDirectory(csvDir);
            }

            // Update backup record with success
            backup.setBackupStatus(BACKUP_STATUS_SUCCESS);
            backup.setBackupSize(backupFile.length());
            saveBackupHistory(backup);

            // Clean up old backups
            cleanupOldBackups();

        } catch (Exception e) {
            e.printStackTrace();

            // Update backup record with failure
            backup.setBackupStatus(BACKUP_STATUS_FAILED);
            backup.setBackupDescription("Error: " + e.getMessage());
            saveBackupHistory(backup);
        }

        return backup;
    }

    /**
     * Perform a SQL backup using mysqldump
     */
    private void performSqlBackup(File backupFile) throws IOException {
        Connection conn = null;
        try (PrintWriter writer = new PrintWriter(new FileWriter(backupFile))) {
            // Get database connection details from the singleton
            String url = "jdbc:mysql://localhost:3307/heartcare";
            String username = "root";
            String password = "Zoro*2222";

            // Extract database name
            String databaseName = url.substring(url.lastIndexOf("/") + 1);

            // Write header
            writer.println("-- HeartCare Database Backup");
            writer.println("-- Generated on: " + LocalDateTime.now());
            writer.println("-- ------------------------------------------------------------");
            writer.println("");

            // Create connection
            conn = DriverManager.getConnection(url, username, password);

            // Disable foreign key checks at the beginning
            writer.println("SET FOREIGN_KEY_CHECKS = 0;");
            writer.println("");

            // Get all tables
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                writer.println("-- ------------------------------------------------------------");
                writer.println("-- Table structure for table `" + tableName + "`");
                writer.println("-- ------------------------------------------------------------");
                writer.println("");

                // Get table create statement
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + tableName + "`");

                if (rs.next()) {
                    String createTable = rs.getString(2);
                    writer.println("DROP TABLE IF EXISTS `" + tableName + "`;");
                    writer.println(createTable + ";");
                    writer.println("");
                }

                // Get table data
                writer.println("-- ------------------------------------------------------------");
                writer.println("-- Data for table `" + tableName + "`");
                writer.println("-- ------------------------------------------------------------");
                writer.println("");

                rs = stmt.executeQuery("SELECT * FROM `" + tableName + "`");
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                // Check if table has data
                boolean hasData = rs.next();
                if (hasData) {
                    StringBuilder insertPrefix = new StringBuilder();
                    insertPrefix.append("INSERT INTO `").append(tableName).append("` (");

                    // Write column names
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) insertPrefix.append(", ");
                        insertPrefix.append("`").append(rsmd.getColumnName(i)).append("`");
                    }

                    insertPrefix.append(") VALUES");
                    writer.println(insertPrefix.toString());

                    // Write data
                    int rowCount = 0;
                    final int MAX_ROWS_PER_INSERT = 100;

                    do {
                        if (rowCount > 0 && rowCount % MAX_ROWS_PER_INSERT == 0) {
                            writer.println(";");
                            writer.println(insertPrefix.toString());
                        } else if (rowCount > 0) {
                            writer.println(",");
                        }

                        StringBuilder row = new StringBuilder("(");

                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) row.append(", ");

                            Object value = rs.getObject(i);
                            if (value == null) {
                                row.append("NULL");
                            } else if (value instanceof Number) {
                                row.append(value.toString());
                            } else if (value instanceof Boolean) {
                                row.append(((Boolean) value) ? "1" : "0");
                            } else if (value instanceof byte[]) {
                                // Handle binary data (e.g., BLOB)
                                byte[] bytes = (byte[]) value;
                                row.append("0x");
                                for (byte b : bytes) {
                                    row.append(String.format("%02X", b));
                                }
                            } else if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                                // Format date/time values
                                String dateStr = value.toString();
                                row.append("'").append(dateStr).append("'");
                            } else {
                                // String and other types - escape single quotes
                                row.append("'").append(value.toString().replace("'", "''")).append("'");
                            }
                        }

                        row.append(")");
                        writer.print(row.toString());

                        rowCount++;
                    } while (rs.next());

                    writer.println(";");
                    writer.println("");
                }

                rs.close();
                stmt.close();
            }

            // Re-enable foreign key checks at the end
            writer.println("SET FOREIGN_KEY_CHECKS = 1;");
            writer.println("");
            writer.println("-- Backup completed on: " + LocalDateTime.now());

            tables.close();

        } catch (SQLException e) {
            throw new IOException("Error creating SQL backup: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Perform a CSV backup
     */
    private void performCsvBackup(File parentDir, String dirName) throws SQLException, IOException {
        // Create a directory for this backup
        File backupDir = new File(parentDir, dirName);
        backupDir.mkdirs();

        Connection conn = DatabaseSingleton.getInstance().getConnection();

        // Export each table to a CSV file
        for (String table : TABLES) {
            File csvFile = new File(backupDir, table + ".csv");

            try (
                    PrintWriter writer = new PrintWriter(new FileWriter(csvFile));
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)
            ) {
                // Get column names
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Write header
                for (int i = 1; i <= columnCount; i++) {
                    writer.print(metaData.getColumnName(i));
                    if (i < columnCount) {
                        writer.print(",");
                    }
                }
                writer.println();

                // Write data
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        String value = rs.getString(i);

                        // Escape commas and quotes in CSV
                        if (value != null) {
                            value = value.replace("\"", "\"\"");

                            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                                value = "\"" + value + "\"";
                            }
                        }

                        writer.print(value != null ? value : "");

                        if (i < columnCount) {
                            writer.print(",");
                        }
                    }
                    writer.println();
                }
            }
        }
    }

    /**
     * Zip a directory
     */
    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            zipDirectory(sourceDir, sourceDir.getName(), zos);
        }
    }

    /**
     * Recursive helper for zipping a directory
     */
    private void zipDirectory(File dir, String basePath, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[1024];
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, basePath + "/" + file.getName(), zos);
                continue;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry entry = new ZipEntry(basePath + "/" + file.getName());
                zos.putNextEntry(entry);

                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
            }
        }
    }

    /**
     * Delete a directory recursively
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * Clean up old backups based on retention policy
     */
    private void cleanupOldBackups() {
        if (backupSchedule == null || backupSchedule.getRetentionDays() <= 0) {
            return;
        }

        // Calculate the cutoff date
        LocalDate cutoffDate = LocalDate.now().minusDays(backupSchedule.getRetentionDays());

        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Get old backups
            String query =
                    "SELECT BackupID, BackupPath FROM backup_history " +
                            "WHERE DATE(BackupDateTime) < ? AND BackupStatus = ?";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(cutoffDate));
            pstmt.setString(2, BACKUP_STATUS_SUCCESS);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int backupId = rs.getInt("BackupID");
                String path = rs.getString("BackupPath");

                // Delete the file
                File backupFile = new File(path);
                if (backupFile.exists()) {
                    backupFile.delete();
                }

                // Update the record
                String updateQuery =
                        "UPDATE backup_history SET BackupStatus = 'Deleted', " +
                                "BackupDescription = 'Deleted based on retention policy' " +
                                "WHERE BackupID = ?";

                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setInt(1, backupId);
                updateStmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save a backup history record
     */
    public boolean saveBackupHistory(BackupHistory backup) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            if (backup.getBackupID() > 0) {
                // Update existing record
                String updateQuery =
                        "UPDATE backup_history SET " +
                                "BackupDateTime = ?, BackupType = ?, BackupFormat = ?, " +
                                "BackupPath = ?, BackupSize = ?, BackupStatus = ?, " +
                                "BackupDescription = ?, CreatedBy = ? " +
                                "WHERE BackupID = ?";

                PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                pstmt.setTimestamp(1, Timestamp.valueOf(backup.getBackupDateTime()));
                pstmt.setString(2, backup.getBackupType());
                pstmt.setString(3, backup.getBackupFormat());
                pstmt.setString(4, backup.getBackupPath());
                pstmt.setLong(5, backup.getBackupSize());
                pstmt.setString(6, backup.getBackupStatus());
                pstmt.setString(7, backup.getBackupDescription());
                pstmt.setString(8, backup.getCreatedBy());
                pstmt.setInt(9, backup.getBackupID());

                int result = pstmt.executeUpdate();
                return result > 0;

            } else {
                // Insert new record
                String insertQuery =
                        "INSERT INTO backup_history " +
                                "(BackupDateTime, BackupType, BackupFormat, BackupPath, " +
                                "BackupSize, BackupStatus, BackupDescription, CreatedBy) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                pstmt.setTimestamp(1, Timestamp.valueOf(backup.getBackupDateTime()));
                pstmt.setString(2, backup.getBackupType());
                pstmt.setString(3, backup.getBackupFormat());
                pstmt.setString(4, backup.getBackupPath());
                pstmt.setLong(5, backup.getBackupSize());
                pstmt.setString(6, backup.getBackupStatus());
                pstmt.setString(7, backup.getBackupDescription());
                pstmt.setString(8, backup.getCreatedBy());

                int result = pstmt.executeUpdate();

                if (result > 0) {
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        backup.setBackupID(generatedKeys.getInt(1));
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get backup history list
     */
    public ObservableList<BackupHistory> getBackupHistory() {
        ObservableList<BackupHistory> backups = FXCollections.observableArrayList();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM backup_history ORDER BY BackupDateTime DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                BackupHistory backup = new BackupHistory(
                        rs.getInt("BackupID"),
                        rs.getTimestamp("BackupDateTime").toLocalDateTime(),
                        rs.getString("BackupType"),
                        rs.getString("BackupFormat"),
                        rs.getString("BackupPath"),
                        rs.getLong("BackupSize"),
                        rs.getString("BackupStatus"),
                        rs.getString("BackupDescription"),
                        rs.getString("CreatedBy")
                );

                backups.add(backup);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return backups;
    }

    /**
     * Execute a backup on application startup if configured
     * @return true if backup was executed, false otherwise
     */
    public boolean executeStartupBackup() {
        // Check if startup backup is enabled in settings
        if (backupSchedule != null && backupSchedule.isBackupOnStartup()) {
            try {
                Task<BackupHistory> backupTask = startBackupTask(
                        BACKUP_TYPE_AUTOMATIC,
                        backupSchedule.getBackupFormat()
                );

                // No need to block the application startup with this
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Execute a backup on application exit if configured
     * This method should be called synchronously during application shutdown
     * @return true if backup was executed, false otherwise
     */
    public boolean executeExitBackup() {
        // Check if exit backup is enabled in settings
        if (backupSchedule != null && backupSchedule.isBackupOnExit()) {
            try {
                // Run synchronously since application is shutting down
                performBackup(
                        BACKUP_TYPE_AUTOMATIC,
                        backupSchedule.getBackupFormat()
                );
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Shutdown the backup scheduler
     */
    public void shutdown() {
        if (scheduledBackup != null) {
            scheduledBackup.cancel(false);
        }

        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}