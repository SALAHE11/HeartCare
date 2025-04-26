package com.example.myjavafxapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.text.Font;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Load custom fonts if needed
        try {
            // Load a custom font if you have one
            // Font.loadFont(getClass().getResourceAsStream("/fonts/YourCustomFont.ttf"), 12);
        } catch (Exception e) {
            System.out.println("Font loading error: " + e.getMessage());
        }

        // Load the FXML file
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("loginForm.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Set application title
        stage.setTitle("HeartCare");

        // Set explicit dimensions of 1024x600
        stage.setWidth(1024);
        stage.setHeight(600);

        // Set minimum dimensions
        stage.setMinWidth(1024);
        stage.setMinHeight(600);

        // Load and resize application icon to standard size
        try {
            // Load the icon image
            Image originalIcon = new Image(getClass().getResourceAsStream("/Icons/iconAppMod3.png"));

            // Create a resized version (64x64 is a common size for application icons)
            // JavaFX will automatically use the appropriate size variant as needed
            Image icon16 = new Image(getClass().getResourceAsStream("/Icons/iconAppMod3.png"), 16, 16, true, true);
            Image icon32 = new Image(getClass().getResourceAsStream("/Icons/iconAppMod3.png"), 32, 32, true, true);
            Image icon64 = new Image(getClass().getResourceAsStream("/Icons/iconAppMod3.png"), 64, 64, true, true);

            // Add all icon sizes to the stage
            // Different platforms will use different sizes, so it's good to provide multiple
            stage.getIcons().addAll(icon16, icon32, icon64, originalIcon);
        } catch (Exception e) {
            System.out.println("Icon loading error: " + e.getMessage());
        }

        // Set the scene
        stage.setScene(scene);

        // Center the stage on screen
        stage.centerOnScreen();

        // Show the window
        stage.show();
    }

    public static void main(String[] args) {
        // Define application name for systems that support it (like macOS)
        System.setProperty("apple.awt.application.name", "HeartCare");

        // Launch the JavaFX application
        launch();
    }
}