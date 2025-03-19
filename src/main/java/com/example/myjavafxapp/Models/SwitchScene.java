package com.example.myjavafxapp.Models;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SwitchScene {
    public static void switchScene(ActionEvent event, String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(SwitchScene.class.getResource(fxmlFile));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Get current dimensions before changing the scene
        double width = stage.getWidth();
        double height = stage.getHeight();

        // Create new scene with the same dimensions
        Scene scene = new Scene(root);

        stage.setScene(scene);

        // Apply the dimensions to the stage to ensure consistency
        stage.setWidth(width);
        stage.setHeight(height);

        stage.show();
    }
}