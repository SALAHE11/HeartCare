package com.example.myjavafxapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("loginForm.fxml"));
        Scene scene = new Scene(fxmlLoader.load(),1024,600);
        // Set minimum window dimensions
        stage.setMinWidth(1024);
        stage.setMinHeight(600);
        stage.setTitle("HeartCare");
//        stage.setResizable(false);

        Image icon = new Image(getClass().getResourceAsStream("/Icons/iconAppMod3.png")); // Adjust the path to your icon file

        // Set the icon for the stage (window)
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();

    }
}