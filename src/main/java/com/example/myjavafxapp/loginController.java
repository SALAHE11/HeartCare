package com.example.myjavafxapp;

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
import java.sql.*;

import java.sql.Connection;
import org.mindrot.jbcrypt.BCrypt;

public class loginController {

    @FXML
    private Label loginError;
    @FXML
    private TextField userNameField;
    @FXML
    private PasswordField passwordField;




    public void loginOnAction(ActionEvent event){
        if(!userNameField.getText().isBlank() && (!passwordField.getText().isBlank()))
        {
            if(validateLogin()){
                try {
                    switchScene(event,"Dashboard.fxml");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                loginError.setText("Incorrect credentials.");
                loginError.setStyle("-fx-text-fill: red;");
            }
        }
        else
            loginError.setText("Please fillout the fields!");

    }

    public boolean validateLogin()  {
     DatabaseConnection connection= new DatabaseConnection();
        Connection conn =connection.getConnection();
        try{
            String sql="SELECT PASSWORD FROM USERS WHERE USERNAME=?";
            PreparedStatement pstm=conn.prepareStatement(sql);
            pstm.setString(1,userNameField.getText());
            ResultSet rs=pstm.executeQuery();
            if(rs.next()){
                String hashedPassword=rs.getString("PASSWORD");
                return verifyPassword(passwordField.getText(),hashedPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void registerAction(ActionEvent event){

        try {
            switchScene(event, "Cin.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void forgotAction(ActionEvent event){
        try {
            switchScene(event,"forgotPassword.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void switchScene(ActionEvent event, String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }


    // Method to verify a password
    private boolean verifyPassword(String plainPassword, String hashedPassword) {
        // Check if the plain password matches the hashed password
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }



}