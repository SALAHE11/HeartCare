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

public class VerifyCodeController {

    @FXML
    private TextField codeField;

    @FXML
    private Label welcomeMessage;

    @FXML
    private Label errorMessage;

    private String username;

    /**
     * Set the username and update the welcome message
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
        updateWelcomeMessage();
    }

    private void updateWelcomeMessage() {
        if (username != null && !username.isEmpty()) {
            welcomeMessage.setText("Vérification pour " + username);
            welcomeMessage.setStyle("-fx-text-fill: green;");
        } else {
            welcomeMessage.setText("Vérification du code");
        }
    }

    @FXML
    private void verifyAction(ActionEvent event) {
        String enteredCode = codeField.getText();

        if (enteredCode == null || enteredCode.isEmpty()) {
            errorMessage.setText("Veuillez entrer le code de vérification.");
            errorMessage.setStyle("-fx-text-fill: orange;");
            return;
        }

        if (EmailUtil.verifyCode(username, enteredCode)) {
            // Code is valid, clear it to prevent reuse
            EmailUtil.clearVerificationCode(username);

            try {
                // Load the reset password FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/auth/resetPassword.fxml"));
                Parent root = loader.load();

                // Get the controller of the reset password scene
                resetPasswordController resetController = loader.getController();

                // Pass the username to the controller
                resetController.setUsername(username);

                // Switch to the reset password scene
                Stage stage = (Stage) codeField.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                errorMessage.setText("Erreur de chargement de la page de réinitialisation.");
                errorMessage.setStyle("-fx-text-fill: red;");
            }
        } else {
            // Check if we have a code for this user at all
            if (!EmailUtil.hasValidCode(username)) {
                errorMessage.setText("Code expiré ou invalide. Veuillez demander un nouveau code.");
            } else {
                errorMessage.setText("Code de vérification incorrect. Veuillez réessayer.");
            }
            errorMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void resendAction(ActionEvent event) {
        if (username == null || username.isEmpty()) {
            errorMessage.setText("Erreur: Aucun utilisateur spécifié.");
            errorMessage.setStyle("-fx-text-fill: red;");
            return;
        }

        // Check if a code was recently sent
        long remainingMinutes = EmailUtil.getRemainingValidityMinutes(username);
        if (remainingMinutes > 10) { // Only allow resend if less than 5 minutes remaining
            errorMessage.setText("Un code a déjà été envoyé. Veuillez attendre " +
                    (remainingMinutes > 1 ? remainingMinutes + " minutes" : "1 minute") +
                    " avant d'en demander un nouveau.");
            errorMessage.setStyle("-fx-text-fill: orange;");
            return;
        }

        // Get user email
        String userEmail = getUserEmail(username);

        if (userEmail != null && !userEmail.isEmpty()) {
            // Generate new verification code
            String newVerificationCode = EmailUtil.generateVerificationCode(username);

            // Send new verification code to user's email
            boolean emailSent = EmailUtil.sendPasswordResetEmail(userEmail, username, newVerificationCode);

            if (emailSent) {
                errorMessage.setText("Un nouveau code a été envoyé à votre adresse e-mail.");
                errorMessage.setStyle("-fx-text-fill: green;");
            } else {
                errorMessage.setText("Erreur d'envoi d'email. Veuillez réessayer plus tard.");
                errorMessage.setStyle("-fx-text-fill: red;");
            }
        } else {
            errorMessage.setText("Erreur: Email non trouvé pour cet utilisateur.");
            errorMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
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