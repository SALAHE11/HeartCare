package com.example.myjavafxapp.Models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseSingleton {

    private static DatabaseSingleton instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3307/heartcare";
    private static final String USER = "root";
    private static final String PASSWORD = "Zoro*2222";  // Change the password here Rihab

    private DatabaseSingleton() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database connection failed.");
        }
    }

    public static DatabaseSingleton getInstance() {
        if (instance == null) {
            instance = new DatabaseSingleton();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}

