package com.example.myjavafxapp.Controllers.Login_inscrire_OublierMDP;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.SwitchScene;
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
import java.sql.SQLException;

public class CinController {

    @FXML
    private TextField textField;
    @FXML
    private Label ErrorMessage;

    private String username;


    // Gets triggered when users submits
    public void submitAction(ActionEvent event){

        Connection conn= DatabaseSingleton.getInstance().getConnection();
         String CIN=textField.getText();
        try {
            PreparedStatement preparedStatement= conn.prepareStatement("SELECT COUNT(1),FNAME,LNAME FROM USERS WHERE ID=?");
            preparedStatement.setString(1,CIN);
            ResultSet rs=preparedStatement.executeQuery();
            if(rs.next()&& rs.getInt(1)>0 && checkIfExists(CIN)==false){
                try {
                    username=rs.getString(2)+" "+rs.getString(3);

                    // Load the registration FXML file
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/Register.fxml"));
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
                ErrorMessage.setText("Veuillez remplir le champ.");
                ErrorMessage.setStyle("-fx-text-fill: orange;");
            }
            else if(checkIfExists(CIN)){
                ErrorMessage.setText("Vous êtes déjà inscrit.");
                ErrorMessage.setStyle("-fx-text-fill: red;");
            }
            else{
                ErrorMessage.setText("CIN indisponible.");
                ErrorMessage.setStyle("-fx-text-fill: red;");
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    //Method that gets triggered when user Canceles
    public void cancelAction(ActionEvent event){

        try {
            SwitchScene.switchScene(event,"/com/example/myjavafxapp/loginForm.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    //  Checks if user exists
    public boolean checkIfExists(String CIN) {

        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        boolean exists = false;

        try {
            conn= DatabaseSingleton.getInstance().getConnection();
            String sql = "SELECT USERNAME FROM USERS WHERE ID = ?";
            pstm = conn.prepareStatement(sql);
            pstm.setString(1, CIN);
            rs = pstm.executeQuery();

            // Check if the ResultSet has any data
            if (rs.next()) {
                String userName = rs.getString("USERNAME");
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return exists;
    }

}
