package com.example.myjavafxapp.Controllers.Login_inscrire_OublierMDP;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.Hashing;
import com.example.myjavafxapp.Models.SwitchScene;
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
            welcomeMessage.setText("Bienvenue, " + username + "!");
            welcomeMessage.setStyle("-fx-text-fill: green;");
        } else {
            welcomeMessage.setText("Bienvenue!");
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


    // Method that validates conditions for username
    private boolean validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            usernameError.setText("Le username ne peut pas être vide.");
            usernameError.setStyle("-fx-text-fill: red;");
            return false;
        }
        else if(username.length()<5){
            usernameError.setText("Le username doit être plus long.");
            usernameError.setStyle("-fx-text-fill: red;");
            return false;
        }
        else if (!isUsernameUnique(username)) {
            usernameError.setText("Ce username existe déjà.");
            usernameError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            usernameError.setText("Username disponible.");
            usernameError.setStyle("-fx-text-fill: green;");
            return true;
        }

    }
    // Method that validates password
    private boolean validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            passwordError.setText("le password ne peut pas être vide.");
            passwordError.setStyle("-fx-text-fill: red;");
            return false;
        } else if (!isPasswordStrong(password)) {
            passwordError.setText("Le password doit comporter au moins 8 caractères, contenir un chiffre, un caractère spécial et une majuscule.");
            passwordError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            passwordError.setText("Le password est fort");
            passwordError.setStyle("-fx-text-fill: green;");
            return true;
        }

    }


    //Method that validates Confirm password
    private boolean validateConfirmPassword(String confirmPassword) {
        String password = passwordField.getText();
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Le mot de passe de confirmation ne peut pas être vide.");
            confirmPasswordError.setStyle("-fx-text-fill: red;");
            return false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordError.setText("Les mots de passe ne correspondent pas");
            confirmPasswordError.setStyle("-fx-text-fill: red;");
            return false;
        } else {
            confirmPasswordError.setText("Les mots de passe correspondent.");
            confirmPasswordError.setStyle("-fx-text-fill: green;");
            return true;
        }
    }


    // Method that checks if username already exists in the Database
    private boolean isUsernameUnique(String username) {

        Connection conn= DatabaseSingleton.getInstance().getConnection();
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


    //Method that checks Conditions for password
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



    // Method that gets triggered when user clicks Register
    @FXML
    private void handleRegistration(ActionEvent event){

        if( !validateUsername(userNameField.getText()) || !validatePassword(passwordField.getText()) || !validateConfirmPassword(ConfirmField.getText()) ){
            welcomeMessage.setText("Certains champs ne sont pas valides !");
            welcomeMessage.setStyle("-fx-text-fill: red;");
        }

        else{
            newUserName=userNameField.getText();
            hashedPassword= Hashing.hashPassword(passwordField.getText());
            String sql="UPDATE USERS SET USERNAME = ?,PASSWORD=? WHERE ID = ?";
            Connection conn= DatabaseSingleton.getInstance().getConnection();
            try {
                PreparedStatement pstm=conn.prepareStatement(sql);
                pstm.setString(1,newUserName);
                pstm.setString(2,hashedPassword);
                pstm.setString(3,CIN);
                int rs =pstm.executeUpdate();
                if(rs>0)
                {
                    SwitchScene.switchScene(event,"/com/example/myjavafxapp/loginForm.fxml");
                }
                else{
                    welcomeMessage.setText("Un problème est survenu!");
                    welcomeMessage.setStyle("-fx-text-fill: red;");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}