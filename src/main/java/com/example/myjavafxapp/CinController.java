package com.example.myjavafxapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CinController {

    @FXML
    private TextField textField;
    @FXML
    private Label ErrorMessage;

    private String username;

    public void submitAction(ActionEvent event){

        DatabaseConnection conn= new DatabaseConnection();
        Connection connection=conn.getConnection();
         String CIN=textField.getText();
        try {
            PreparedStatement preparedStatement= connection.prepareStatement("SELECT COUNT(1),firstName,lastName FROM USERS WHERE USER_ID=?");
            preparedStatement.setString(1,CIN);
            ResultSet rs=preparedStatement.executeQuery();
            if(rs.next()&& rs.getInt(1)>0 && checkIfExists(CIN)==false){
                try {
                    username=rs.getString(2)+" "+rs.getString(3);

                    // Load the registration FXML file
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("Register.fxml"));
                    Parent root=loader.load();

                    // get the controller of the registration scene
                    registerController registerController= loader.getController();

                    //pass the username to the controller
                    registerController.setUsername(username,CIN);

                    //switch to the registration scene
                    Stage stage = (Stage) textField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
             }

            } else if (textField.getText().isBlank()) {
                ErrorMessage.setText("Please fillout the field.");
                ErrorMessage.setStyle("-fx-text-fill: orange;");
            }
            else if(checkIfExists(CIN)){
                ErrorMessage.setText("You are already registered.");
                ErrorMessage.setStyle("-fx-text-fill: red;");
            }
            else{
                ErrorMessage.setText("Unavailable CIN.");
                ErrorMessage.setStyle("-fx-text-fill: red;");
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void cancelAction(ActionEvent event){

        try {
            switchScene(event,"loginForm.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void switchScene(ActionEvent event, String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public boolean checkIfExists(String CIN) {
        DatabaseConnection dbConnection = new DatabaseConnection();
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        boolean exists = false;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT userName FROM USERS WHERE USER_ID = ?";
            pstm = conn.prepareStatement(sql);
            pstm.setString(1, CIN);
            rs = pstm.executeQuery();

            // Check if the ResultSet has any data
            if (rs.next()) {
                String userName = rs.getString("userName");
                // If userName is not null, the user exists
                exists = (userName != null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources in the reverse order of their creation
            try {
                if (rs != null) rs.close();
                if (pstm != null) pstm.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return exists;
    }

}
