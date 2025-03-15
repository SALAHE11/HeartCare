package com.example.myjavafxapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import org.mindrot.jbcrypt.BCrypt;


public class registerController {

    @FXML
    private TextField userNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField ConfirmField;

    @FXML
    private Label welcomeMessage;

    @FXML
    private Label usernameError;

    @FXML
    private Label passwordError;

    @FXML
    private Label confirmPasswordError;

    private String username;
    private String CIN;
    private String newUserName;
    private String hashedPassword;

    // Method to set the username and update the welcome message
    public void setUsername(String username, String CIN) {
        this.username = username;
        updateWelcomeMessage();
        this.CIN=CIN;
    }


    private void updateWelcomeMessage() {
        if (username != null && !username.isEmpty()) {
            welcomeMessage.setText("Welcome, " + username + "!");
            welcomeMessage.setStyle("-fx-text-fill: green;");
        } else {
            welcomeMessage.setText("Welcome!");
        }
    }

    @FXML
    private void initialize() {
        // Add listeners to the text fields for real-time validation
        userNameField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                validateUsername(newValue);
            }
        });

        passwordField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                validatePassword(newValue);
            }
        });

        ConfirmField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                validateConfirmPassword(newValue);
            }
        });
    }

    private boolean validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            usernameError.setText("Username cannot be empty.");
            usernameError.setStyle("-fx-text-fill: red;");
            return false;
        }
        else if(username.length()<5){
            usernameError.setText("Username length must be bigger than 5.");
            usernameError.setStyle("-fx-text-fill: red;");
            return false;
        }
        else if (!isUsernameUnique(username)) {
            usernameError.setText("Username already exists.");
            usernameError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            usernameError.setText("Username is available.");
            usernameError.setStyle("-fx-text-fill: green;");
            return true;
        }

    }

    private boolean validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            passwordError.setText("Password cannot be empty.");
            passwordError.setStyle("-fx-text-fill: red;");
            return false;
        } else if (!isPasswordStrong(password)) {
            passwordError.setText("Password must be at least 8 characters long, contain a number, a special character, and a capital letter.");
            passwordError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            passwordError.setText("Password is strong.");
            passwordError.setStyle("-fx-text-fill: green;");
            return true;
        }

    }

    private boolean validateConfirmPassword(String confirmPassword) {
        String password = passwordField.getText();
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Confirm password cannot be empty.");
            confirmPasswordError.setStyle("-fx-text-fill: red;");
            return false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordError.setText("Passwords do not match.");
            confirmPasswordError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            confirmPasswordError.setText("Passwords match.");
            confirmPasswordError.setStyle("-fx-text-fill: green;");
            return true;
        }
    }

    private boolean isUsernameUnique(String username) {

        DatabaseConnection dbConnection=new DatabaseConnection();
        Connection conn=dbConnection.getConnection();
        String sql="SELECT COUNT(1) FROM USERS WHERE USERNAME=?";
        boolean bool=false;
        try
        {
            PreparedStatement pstm=conn.prepareStatement(sql);
            pstm.setString(1,username);
            ResultSet rs=pstm.executeQuery();
            if(rs.next() && rs.getInt(1)>0)
                bool= false;
            else
                bool= true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bool;
    }

    private boolean isPasswordStrong(String password) {
        // Password must be at least 8 characters long
        if (password.length() < 8) {
            return false;
        }

        // Password must contain at least one number
        if (!password.matches(".*[0-9].*")) {
            return false;
        }

        // Password must contain at least one special character
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return false;
        }

        // Password must contain at least one capital letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        return true;
    }

    // Hey salaheddine this is the method that hashes the password
    private String hashPassword(String plainPassword){
        // Generate a salt and hashes the password
        return BCrypt.hashpw(plainPassword,BCrypt.gensalt());
    }


    // this is the method that will handle registration
    @FXML
    private void handleRegistration(ActionEvent event){

        if( !validateUsername(userNameField.getText()) || !validatePassword(passwordField.getText()) || !validateConfirmPassword(ConfirmField.getText()) ){
            welcomeMessage.setText("Certain fields are not valid!");
            welcomeMessage.setStyle("-fx-text-fill: red;");
        }

        else{
            newUserName=userNameField.getText();
            hashedPassword=hashPassword(passwordField.getText());
            String sql="UPDATE USERS SET USERNAME = ?,PASSWORD=? WHERE USER_ID = ?";
            DatabaseConnection dbconn=new DatabaseConnection();
            Connection conn=dbconn.getConnection();
            try {
                PreparedStatement pstm=conn.prepareStatement(sql);
                pstm.setString(1,newUserName);
                pstm.setString(2,hashedPassword);
                pstm.setString(3,CIN);
                int rs =pstm.executeUpdate();
                if(rs>0)
                {
                    welcomeMessage.setText("You have registered succefully!");
                    welcomeMessage.setStyle("-fx-text-fill: green;");
                }
                else{
                    welcomeMessage.setText("A problem occured");
                    welcomeMessage.setStyle("-fx-text-fill: red;");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}