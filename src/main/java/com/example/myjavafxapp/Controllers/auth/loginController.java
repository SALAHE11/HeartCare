package com.example.myjavafxapp.Controllers.auth;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.util.Hashing;
import com.example.myjavafxapp.Models.util.SwitchScene;
import com.example.myjavafxapp.Models.user.UserSession;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class loginController {

    @FXML
    private Label loginError;
    @FXML
    private TextField userNameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private FontIcon eyeIcon;

    private boolean passwordVisible = false;

    @FXML
    private void initialize() {
        // Bind the passwordField and passwordVisibleField to stay in sync
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            passwordVisibleField.setText(newValue);
        });

        passwordVisibleField.textProperty().addListener((observable, oldValue, newValue) -> {
            passwordField.setText(newValue);
        });
    }

    @FXML
    public void togglePasswordVisibility(MouseEvent event) {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            // Show password
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            eyeIcon.setIconLiteral("fas-eye");
        } else {
            // Hide password
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            eyeIcon.setIconLiteral("fas-eye-slash");
        }
    }

    public void loginOnAction(ActionEvent event) {
        String password = passwordVisible ? passwordVisibleField.getText() : passwordField.getText();

        if (!userNameField.getText().isBlank() && !password.isBlank()) {
            if (validateLogin(password)) {
                try {
                    // Store username in session
                    UserSession session = UserSession.getInstance();
                    session.setUsername(userNameField.getText());

                    // Fetch and store user role
                    fetchUserRole();

                    SwitchScene.switchScene(event, "/com/example/myjavafxapp/appointments/CalendarView.fxml");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                loginError.setText("Identification incorrecte.");
                loginError.setStyle("-fx-text-fill: red;");
            }
        } else {
            loginError.setText("Veuillez remplir les champs!");
        }
    }

    private boolean validateLogin(String password) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            String sql = "SELECT password FROM users WHERE username = ?";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, userNameField.getText());
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return Hashing.verifyPassword(password, hashedPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void fetchUserRole() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            String sql = "SELECT role FROM users WHERE username = ?";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, userNameField.getText());
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                UserSession.getInstance().setRole(role);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerAction(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/auth/Cin.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void forgotAction(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/auth/forgotPassword.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}