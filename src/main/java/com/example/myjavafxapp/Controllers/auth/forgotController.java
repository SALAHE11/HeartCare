package com.example.myjavafxapp.Controllers.auth;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.util.EmailUtil;
import com.example.myjavafxapp.Models.util.SwitchScene;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class forgotController {

    @FXML
    private TextField usernameField;

    @FXML
    private Label errorMessage;

    @FXML
    // Gets triggered when user submits
    private void submitAction(ActionEvent event) {
        if (!usernameField.getText().isEmpty()) {
            String username = usernameField.getText();

            // Check if username exists and get email
            String userEmail = getUserEmail(username);

            if (userEmail != null && !userEmail.isEmpty()) {
                // Generate verification code
                String verificationCode = EmailUtil.generateVerificationCode(username);

                // Send verification code to user's email
                boolean emailSent = EmailUtil.sendPasswordResetEmail(userEmail, username, verificationCode);

                if (emailSent) {
                    try {
                        // Load the verification code FXML
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/auth/verifyCode.fxml"));
                        Parent root = loader.load();

                        // Get the controller of the verification scene
                        VerifyCodeController verifyController = loader.getController();

                        // Pass the username to the controller
                        verifyController.setUsername(username);

                        // Switch to the verification scene
                        Stage stage = (Stage) usernameField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        errorMessage.setText("Erreur de chargement de la page de vérification.");
                        errorMessage.setStyle("-fx-text-fill: red;");
                    }
                } else {
                    errorMessage.setText("Erreur d'envoi d'email. Veuillez réessayer plus tard.");
                    errorMessage.setStyle("-fx-text-fill: red;");
                }
            } else {
                errorMessage.setText("Nom d'utilisateur introuvable ou email non configuré.");
                errorMessage.setStyle("-fx-text-fill: red;");
            }
        } else {
            errorMessage.setText("Veuillez entrer votre nom d'utilisateur.");
            errorMessage.setStyle("-fx-text-fill: orange;");
        }
    }

    @FXML
    // Gets triggered when user cancels
    private void cancelAction(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/auth/loginForm.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to retrieve user email from database
    private String getUserEmail(String username) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        String email = null;

        try {
            String sql = "SELECT EMAIL FROM USERS WHERE USERNAME = ?";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, username);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                email = rs.getString("EMAIL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return email;
    }
}