package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.SwitchScene;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class forgotController {

    @FXML
    private TextField textField;

    @FXML
    private DatePicker dateField;

    @FXML
    private Label ErrorMessage;

    @FXML
    // gets triggered when user submits
    private void submitAction(ActionEvent event) {
        if (!textField.getText().isEmpty() && dateField.getValue() != null) {
            LocalDate selectedDate = dateField.getValue();
            String dateString = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            validateInfos(textField.getText(), dateString);

        } else {
            ErrorMessage.setText("veuillez remplir les champs.");
            ErrorMessage.setStyle("-fx-text-fill: orange;");
        }
    }

    @FXML
    // gets triggered when user canceles
    private void cancelAction(ActionEvent event) {
        try {
            SwitchScene.switchScene(event,"/com/example/myjavafxapp/loginForm.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //checks if user exists
    private void validateInfos(String username, String date) {
        Connection conn= DatabaseSingleton.getInstance().getConnection();
        boolean isValid = false;
        String sql = "SELECT count(1),USERNAME FROM USERS WHERE USERNAME=? AND BIRTHDATE=?";
        try {
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, username);
            pstm.setString(2, date);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()&& rs.getInt(1)>0) {
                String retrievedUsername = rs.getString("USERNAME");
                try {
                    // Load the registration FXML file
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/resetPassword.fxml"));
                    Parent root = loader.load();

                    // Get the controller of the registration scene
                    resetPasswordController renterController = loader.getController();

                    // Pass the username to the controller
                    renterController.setUsername(retrievedUsername);

                    // Switch to the registration scene
                    Stage stage = (Stage) textField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                ErrorMessage.setText("Identifications incorrectes.");
                ErrorMessage.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}