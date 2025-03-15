package com.example.myjavafxapp;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    public Connection conn;
    public Connection getConnection(){
        String dbName="bank";
        String dbUser="root";
        String dbPassword="Zoro*2222";
        String url="jdbc:mysql://localhost:3307/"+dbName;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn=DriverManager.getConnection(url,dbUser,dbPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
}
