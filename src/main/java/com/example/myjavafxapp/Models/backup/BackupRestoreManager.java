package com.example.myjavafxapp.Models.backup;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.*;

/**
 * Manager class for restore operations
 */
public class BackupRestoreManager {
    private static BackupRestoreManager instance;

    private BackupRestoreManager() {}

    public static synchronized BackupRestoreManager getInstance() {
        if (instance == null) {
            instance = new BackupRestoreManager();
        }
        return instance;
    }

    /**
     * Restore a database from a SQL backup file
     */
    public boolean restoreFromSql(String filePath) throws IOException, InterruptedException, SQLException {
        // First, verify the file exists
        File backupFile = new File(filePath);
        if (!backupFile.exists()) {
            throw new FileNotFoundException("Backup file not found: " + filePath);
        }

        // Get database connection details
        String url = "jdbc:mysql://localhost:3307/heartcare";
        String username = "root";
        String password = "Zoro*2222";

        // Extract database parameters from URL
        String[] parts = url.split("/");
        String database = parts[parts.length - 1];
        String host = url.split("://")[1].split(":")[0];
        String port = url.split(":")[2].split("/")[0];

        // Close any existing connections
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }

        // Restore using mysql command
        ProcessBuilder pb = new ProcessBuilder(
                "mysql",
                "--host=" + host,
                "--port=" + port,
                "--user=" + username,
                "--password=" + password,
                database
        );

        // Redirect input from the SQL file
        pb.redirectInput(backupFile);

        // Log the restore operation
        logRestoreOperation(filePath, "SQL");

        // Execute the command
        Process process = pb.start();
        int exitCode = process.waitFor();

        // Refresh the database connection
        DatabaseSingleton.refreshConnection();

        return exitCode == 0;
    }

    /**
     * Restore a database from a CSV backup (ZIP file containing CSVs)
     */
    public boolean restoreFromCsv(String zipFilePath) throws IOException, SQLException {
        // First, verify the file exists
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            throw new FileNotFoundException("Backup file not found: " + zipFilePath);
        }

        // Create a temporary directory for extraction
        Path tempDir = Files.createTempDirectory("heartcare_restore_");

        try {
            // Extract the ZIP file
            extractZipFile(zipFile, tempDir.toFile());

            // Close any existing connections
            Connection conn = DatabaseSingleton.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }

            // Restore the database from CSV files
            restoreDatabaseFromCsvs(tempDir.toFile());

            // Log the restore operation
            logRestoreOperation(zipFilePath, "CSV");

            // Refresh the database connection
            DatabaseSingleton.refreshConnection();

            return true;

        } finally {
            // Clean up the temporary directory
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * Extract a ZIP file to a directory
     */
    private void extractZipFile(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());

                // Create parent directories if they don't exist
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Ensure parent directories exist
                    new File(newFile.getParent()).mkdirs();

                    // Extract the file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
    }

    /**
     * Restore database tables from CSV files
     */
    private void restoreDatabaseFromCsvs(File csvDir) throws SQLException, IOException {
        // Get a list of CSV files in the directory
        File[] csvFiles = csvDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            throw new FileNotFoundException("No CSV files found in the backup");
        }

        // Get a new database connection
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3307/heartcare", "root", "Zoro*2222");

        try {
            // Disable foreign key checks to avoid constraint errors
            Statement stmt = conn.createStatement();
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Restore each table
            for (File csvFile : csvFiles) {
                // Table name is the file name without extension
                String tableName = csvFile.getName().substring(0, csvFile.getName().lastIndexOf('.'));

                // Get table structure to determine column types
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet columnsRs = metaData.getColumns(null, null, tableName, null);

                // Read the CSV file
                try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                    // Read header line to get column names
                    String headerLine = reader.readLine();
                    if (headerLine == null) {
                        continue; // Empty file
                    }

                    String[] columns = headerLine.split(",");

                    // Truncate the table
                    stmt.execute("TRUNCATE TABLE " + tableName);

                    // Prepare SQL for inserting rows
                    StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
                    for (int i = 0; i < columns.length; i++) {
                        insertSql.append(columns[i]);
                        if (i < columns.length - 1) {
                            insertSql.append(", ");
                        }
                    }
                    insertSql.append(") VALUES (");
                    for (int i = 0; i < columns.length; i++) {
                        insertSql.append("?");
                        if (i < columns.length - 1) {
                            insertSql.append(", ");
                        }
                    }
                    insertSql.append(")");

                    // Parse and insert each line
                    String line;
                    PreparedStatement pstmt = conn.prepareStatement(insertSql.toString());

                    while ((line = reader.readLine()) != null) {
                        // Parse CSV line
                        String[] values = parseCsvLine(line);

                        // Skip if number of values doesn't match number of columns
                        if (values.length != columns.length) {
                            continue;
                        }

                        // Populate prepared statement
                        for (int i = 0; i < values.length; i++) {
                            pstmt.setString(i + 1, values[i]);
                        }

                        // Execute insert
                        pstmt.executeUpdate();
                    }
                }
            }

            // Re-enable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

        } finally {
            // Close connection
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
    }

    /**
     * Parse a CSV line respecting quoted values
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Check if this is a double quote within quotes
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // Skip the next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                values.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        // Add the last field
        values.add(sb.toString());

        return values.toArray(new String[0]);
    }

    /**
     * Log a restore operation
     */
    private void logRestoreOperation(String filePath, String format) {
        try {
            BackupHistory restoreLog = new BackupHistory();
            restoreLog.setBackupDateTime(LocalDateTime.now());
            restoreLog.setBackupType("Restore");
            restoreLog.setBackupFormat(format);
            restoreLog.setBackupPath(filePath);
            restoreLog.setBackupStatus("Success");
            restoreLog.setBackupDescription("Database restore from " + format + " backup");

            // Get current user
            String username = "System";
            try {
                username = com.example.myjavafxapp.Models.user.UserSession.getInstance().getUsername();
            } catch (Exception e) {
                // Use default
            }
            restoreLog.setCreatedBy(username);

            // Save to database - need a direct connection since we're likely in the middle of reconnecting
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3307/heartcare", "root", "Zoro*2222");

            String insertQuery =
                    "INSERT INTO backup_history " +
                            "(BackupDateTime, BackupType, BackupFormat, BackupPath, " +
                            "BackupSize, BackupStatus, BackupDescription, CreatedBy) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(insertQuery);
            pstmt.setTimestamp(1, Timestamp.valueOf(restoreLog.getBackupDateTime()));
            pstmt.setString(2, restoreLog.getBackupType());
            pstmt.setString(3, restoreLog.getBackupFormat());
            pstmt.setString(4, restoreLog.getBackupPath());
            pstmt.setLong(5, 0); // Size not applicable for restore
            pstmt.setString(6, restoreLog.getBackupStatus());
            pstmt.setString(7, restoreLog.getBackupDescription());
            pstmt.setString(8, restoreLog.getCreatedBy());

            pstmt.executeUpdate();
            conn.close();

        } catch (Exception e) {
            // Log but continue - not critical
            e.printStackTrace();
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
}