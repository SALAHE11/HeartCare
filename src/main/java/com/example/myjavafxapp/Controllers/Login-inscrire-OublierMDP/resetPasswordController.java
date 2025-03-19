package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.Hashing;
import com.example.myjavafxapp.Models.SwitchScene;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class resetPasswordController {

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
            welcomeMessage.setText("Bienvenue, " + username + "!");
            welcomeMessage.setStyle("-fx-text-fill: green;");
        } else {
            welcomeMessage.setText("Bienvenue!");
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


    // Method that get continuously by its listener
    private boolean validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            passwordError.setText("Le mot de passe ne peut pas être vide!");
            passwordError.setStyle("-fx-text-fill: red;");
            return false;
        } else if (!isPasswordStrong(password)) {
            passwordError.setText("Le mot de passe doit comporter au moins 8 caractères, contenir un chiffre, un caractère spécial et une majuscule.");
            passwordError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            passwordError.setText("Le mot de passe est fort.");
            passwordError.setStyle("-fx-text-fill: green;");
            return true;
        }
    }


    // Method that get continuously by its listener
    private boolean validateConfirmPassword(String confirmPassword) {
        String password = passwordField.getText();
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Le password de confirmation ne peut pas être vide!");
            confirmPasswordError.setStyle("-fx-text-fill: red;");
            return false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordError.setText("Les mots de passe ne correspondent pas!");
            confirmPasswordError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            confirmPasswordError.setText("Les mots de passe correspondent.");
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


    @FXML
    //Method that gets triggered when user clicks register
    private void handleRegistration(ActionEvent event) {
        if (!validatePassword(passwordField.getText()) || !validateConfirmPassword(ConfirmField.getText())) {
            welcomeMessage.setText("Certains champs ne sont pas valides!");
            welcomeMessage.setStyle("-fx-text-fill: red;");
        } else {
            hashedPassword = Hashing.hashPassword(passwordField.getText());
            String sql = "UPDATE USERS SET PASSWORD=? WHERE USERNAME = ?";
            Connection conn = DatabaseSingleton.getInstance().getConnection();
            try {
                PreparedStatement pstm = conn.prepareStatement(sql);
                pstm.setString(1, hashedPassword);
                pstm.setString(2, username);
                int rs = pstm.executeUpdate();
                if (rs > 0) {
                    SwitchScene.switchScene(event,"/com/example/myjavafxapp/loginForm.fxml");
                } else {
                    welcomeMessage.setText("Un problème est survenu.");
                    welcomeMessage.setStyle("-fx-text-fill: red;");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}