package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.DatabaseSingleton;
import com.example.myjavafxapp.Models.SwitchScene;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AjouterPatientController implements Initializable {

    @FXML
    private TextField cinField;

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private DatePicker dateNaissanceField;

    @FXML
    private ComboBox<String> sexeComboBox;

    @FXML
    private TextField adresseField;

    @FXML
    private TextField telephoneField;

    @FXML
    private TextField emailField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize the sexe ComboBox with options
        sexeComboBox.setItems(FXCollections.observableArrayList("Male", "Female"));

        // Set up DatePicker format
        dateNaissanceField.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });
    }

    @FXML
    public void savePatient(ActionEvent event) {
        if (validateFields()) {
            try {
                Connection conn = DatabaseSingleton.getInstance().getConnection();
                String insertQuery = "INSERT INTO patient (ID, FNAME, LNAME, BIRTHDATE, SEXE, ADRESSE, TELEPHONE, EMAIL) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement pstmt = conn.prepareStatement(insertQuery);
                pstmt.setString(1, cinField.getText());
                pstmt.setString(2, nomField.getText());
                pstmt.setString(3, prenomField.getText());
                pstmt.setDate(4, Date.valueOf(dateNaissanceField.getValue()));
                pstmt.setString(5, sexeComboBox.getValue());
                pstmt.setString(6, adresseField.getText());
                pstmt.setInt(7, Integer.parseInt(telephoneField.getText()));
                pstmt.setString(8, emailField.getText());

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Patient ajouté avec succès");
                    returnAction(event);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout du patient");
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de format", "Le numéro de téléphone doit être un nombre");
            }
        }
    }

    private boolean validateFields() {
        StringBuilder errorMessage = new StringBuilder();

        if (cinField.getText().isEmpty()) {
            errorMessage.append("Le CIN est obligatoire\n");
        }

        if (nomField.getText().isEmpty()) {
            errorMessage.append("Le nom est obligatoire\n");
        }

        if (prenomField.getText().isEmpty()) {
            errorMessage.append("Le prénom est obligatoire\n");
        }

        if (dateNaissanceField.getValue() == null) {
            errorMessage.append("La date de naissance est obligatoire\n");
        }

        if (sexeComboBox.getValue() == null) {
            errorMessage.append("Le sexe est obligatoire\n");
        }

        if (telephoneField.getText().isEmpty()) {
            errorMessage.append("Le téléphone est obligatoire\n");
        } else {
            try {
                Integer.parseInt(telephoneField.getText());
            } catch (NumberFormatException e) {
                errorMessage.append("Le téléphone doit être un nombre\n");
            }
        }

        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", errorMessage.toString());
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void cancelAction(ActionEvent event) {
        returnAction(event);
    }

    @FXML
    public void returnAction(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/gestionDossierPatient.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}