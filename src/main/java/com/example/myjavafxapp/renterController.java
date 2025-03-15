package com.example.myjavafxapp;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class renterController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField ConfirmField;

    @FXML
    private Label welcomeMessage;

    @FXML
    private Label passwordError;

    @FXML
    private Label confirmPasswordError;

    private String username;
    private String hashedPassword;

    // Method to set the username and update the welcome message
    public void setUsername(String username) {
        this.username = username;
        updateWelcomeMessage();
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

    private String hashPassword(String plainPassword) {
        // Generate a salt and hashes the password
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    @FXML
    private void handleRegistration(ActionEvent event) {
        if (!validatePassword(passwordField.getText()) || !validateConfirmPassword(ConfirmField.getText())) {
            welcomeMessage.setText("Certain fields are not valid!");
            welcomeMessage.setStyle("-fx-text-fill: red;");
        } else {
            hashedPassword = hashPassword(passwordField.getText());
            String sql = "UPDATE USERS SET PASSWORD=? WHERE USERNAME = ?";
            DatabaseConnection dbconn = new DatabaseConnection();
            Connection conn = dbconn.getConnection();
            try {
                PreparedStatement pstm = conn.prepareStatement(sql);
                pstm.setString(1, hashedPassword);
                pstm.setString(2, username);
                int rs = pstm.executeUpdate();
                if (rs > 0) {
                    welcomeMessage.setText("Password changed successfully!");
                    welcomeMessage.setStyle("-fx-text-fill: green;");
                } else {
                    welcomeMessage.setText("A problem occurred.");
                    welcomeMessage.setStyle("-fx-text-fill: red;");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}