package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.Hashing;
import com.example.myjavafxapp.Models.SwitchScene;
import com.example.myjavafxapp.Models.UserSession;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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

    public void loginOnAction(ActionEvent event) {
        if (!userNameField.getText().isBlank() && !passwordField.getText().isBlank()) {
            if (validateLogin()) {
                try {
                    // Store username in session
                    UserSession session = UserSession.getInstance();
                    session.setUsername(userNameField.getText());

                    // Fetch and store user role
                    fetchUserRole();

                    SwitchScene.switchScene(event, "/com/example/myjavafxapp/Dashboard.fxml");
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

    private boolean validateLogin() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            String sql = "SELECT password FROM users WHERE username = ?";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, userNameField.getText());
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return Hashing.verifyPassword(passwordField.getText(), hashedPassword);
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
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/Cin.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void forgotAction(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/forgotPassword.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}