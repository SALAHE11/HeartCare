// Update to DatabaseSingleton.java to add refreshConnection method

package com.example.myjavafxapp.Models.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseSingleton {

    private static DatabaseSingleton instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3307/heartcare";
    private static final String USER = "root";
    private static final String PASSWORD = "Zoro*2222";

    private DatabaseSingleton() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database connection failed.");
        }
    }

    public static synchronized DatabaseSingleton getInstance() {
        if (instance == null) {
            instance = new DatabaseSingleton();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes and re-establishes the database connection.
     * This is useful when restoring from a backup.
     */
    public static synchronized void refreshConnection() {
        if (instance != null) {
            try {
                if (instance.connection != null && !instance.connection.isClosed()) {
                    instance.connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Create a new instance with a fresh connection
            instance = null;
            getInstance();
        }
    }
}