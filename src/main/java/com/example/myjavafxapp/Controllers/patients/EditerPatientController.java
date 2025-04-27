package com.example.myjavafxapp.Controllers.patients;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.patient.Patient;
import com.example.myjavafxapp.Models.patient.PatientDataHolder;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class EditerPatientController implements Initializable {

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

    private Patient currentPatient;

    // Pattern for email validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$");

    // Pattern for phone validation (9 digits starting with 6 or 7)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[67]\\d{8}$");

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

        // Load patient data
        loadPatientData();
    }

    private void loadPatientData() {
        currentPatient = PatientDataHolder.getInstance().getCurrentPatient();
        if (currentPatient != null) {
            cinField.setText(currentPatient.getID());
            // Disable CIN field since it's the primary key
            cinField.setEditable(false);

            nomField.setText(currentPatient.getFNAME());
            prenomField.setText(currentPatient.getLNAME());
            dateNaissanceField.setValue(LocalDate.parse(currentPatient.getBIRTHDATE()));
            sexeComboBox.setValue(currentPatient.getSEXE());
            adresseField.setText(currentPatient.getADRESSE());
            telephoneField.setText(currentPatient.getTELEPHONE().toString());
            emailField.setText(currentPatient.getEMAIL());
        }
    }

    @FXML
    public void updatePatient(ActionEvent event) {
        if (validateFields()) {
            try {
                Connection conn = DatabaseSingleton.getInstance().getConnection();
                String updateQuery = "UPDATE patient SET FNAME=?, LNAME=?, BIRTHDATE=?, SEXE=?, ADRESSE=?, TELEPHONE=?, EMAIL=? WHERE ID=?";

                PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                pstmt.setString(1, nomField.getText());
                pstmt.setString(2, prenomField.getText());
                pstmt.setDate(3, Date.valueOf(dateNaissanceField.getValue()));
                pstmt.setString(4, sexeComboBox.getValue());
                pstmt.setString(5, adresseField.getText());
                pstmt.setInt(6, Integer.parseInt(telephoneField.getText()));
                pstmt.setString(7, emailField.getText());
                pstmt.setString(8, cinField.getText());

                int result = pstmt.executeUpdate();
                if (result > 0) {
                    // Update the current patient in PatientDataHolder with the new values
                    updatePatientDataHolder();

                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Patient modifié avec succès");
                    closeWindow(event);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification du patient");
                }
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de format", "Le numéro de téléphone doit être un nombre");
            }
        }
    }

    private void updatePatientDataHolder() {
        // Update the patient data in the data holder
        if (currentPatient != null) {
            currentPatient.setFNAME(nomField.getText());
            currentPatient.setLNAME(prenomField.getText());
            currentPatient.setBIRTHDATE(dateNaissanceField.getValue().toString());
            currentPatient.setSEXE(sexeComboBox.getValue());
            currentPatient.setADRESSE(adresseField.getText());
            currentPatient.setTELEPHONE(Integer.parseInt(telephoneField.getText()));
            currentPatient.setEMAIL(emailField.getText());

            PatientDataHolder.getInstance().setCurrentPatient(currentPatient);
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
        } else {
            // Validate minimum age of 16 years
            LocalDate birthdate = dateNaissanceField.getValue();
            LocalDate today = LocalDate.now();
            int age = Period.between(birthdate, today).getYears();

            if (age < 16) {
                errorMessage.append("Le patient doit avoir au moins 16 ans\n");
            }
        }

        if (sexeComboBox.getValue() == null) {
            errorMessage.append("Le sexe est obligatoire\n");
        }

        if (telephoneField.getText().isEmpty()) {
            errorMessage.append("Le téléphone est obligatoire\n");
        } else {
            // Validate phone format: 9 digits starting with 6 or 7
            String phone = telephoneField.getText();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                errorMessage.append("Le numéro de téléphone doit comporter exactement 9 chiffres et commencer par 6 ou 7\n");
            }
        }

        if (emailField.getText() != null && !emailField.getText().isEmpty()) {
            // Validate email format
            String email = emailField.getText();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                errorMessage.append("L'adresse email n'est pas valide\n");
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
        closeWindow(event);
    }

    @FXML
    public void returnAction(ActionEvent event) {
        closeWindow(event);
    }

    /**
     * Close the current window/dialog
     */
    private void closeWindow(ActionEvent event) {
        // Get the source of the event
        Node source = (Node) event.getSource();
        // Get the stage (window) that contains the source
        Stage stage = (Stage) source.getScene().getWindow();
        // Close the window
        stage.close();
    }
}