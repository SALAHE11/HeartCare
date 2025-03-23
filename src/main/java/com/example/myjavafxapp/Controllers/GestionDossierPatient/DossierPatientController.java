package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class DossierPatientController implements Initializable {

    @FXML
    private Label patientInfoLabel;

    @FXML
    private GridPane medicalInfoGrid;

    @FXML
    private GridPane additionalInfoGrid;

    @FXML
    private GridPane datesAndInsuranceGrid;

    @FXML
    private ComboBox<String> bloodTypeComboBox;

    @FXML
    private TextArea allergiesField;

    @FXML
    private TextArea currentMedicationsField;

    @FXML
    private TextField bloodPressureField;

    @FXML
    private TextArea chronicConditionsField;

    @FXML
    private TextArea previousSurgeriesField;

    @FXML
    private TextArea familyMedicalHistoryField;

    @FXML
    private TextField dateCreatedField;

    @FXML
    private TextField lastUpdatedField;

    @FXML
    private TextField insuranceProviderField;

    @FXML
    private TextField insurancePolicyNumberField;

    @FXML
    private Button saveButton;

    private Patient currentPatient;
    private DossierPatient currentDossier;
    private boolean isNewDossier = true;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize the blood type ComboBox with options
        bloodTypeComboBox.setItems(FXCollections.observableArrayList(
                "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));

        // Get current patient from the data holder
        currentPatient = PatientDataHolder.getInstance().getCurrentPatient();

        if (currentPatient != null) {
            // Update the patient info label
            patientInfoLabel.setText("CIN: " + currentPatient.getID() + " - Nom: " +
                    currentPatient.getFNAME() + " " + currentPatient.getLNAME());

            // Load patient's medical record if exists
            loadDossierPatient();

            // Apply role-based access control
            applyRoleBasedAccess();
        }
    }

    private void loadDossierPatient() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        String query = "SELECT * FROM dossierpatient WHERE PatientID = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, currentPatient.getID());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                isNewDossier = false;
                currentDossier = new DossierPatient();
                currentDossier.setDossierID(rs.getInt("DossierID"));
                currentDossier.setPatientID(rs.getString("PatientID"));
                currentDossier.setBloodType(rs.getString("BloodType"));
                currentDossier.setAllergies(rs.getString("Allergies"));
                currentDossier.setMedicalHistory(rs.getString("MedicalHistory"));
                currentDossier.setCurrentMedications(rs.getString("CurrentMedications"));
                currentDossier.setDateCreated(rs.getTimestamp("DateCreated"));
                currentDossier.setLastUpdated(rs.getTimestamp("LastUpdated"));
                currentDossier.setBloodPressure(rs.getString("BloodPressure"));
                currentDossier.setChronicConditions(rs.getString("ChronicConditions"));
                currentDossier.setPreviousSurgeries(rs.getString("PreviousSurgeries"));
                currentDossier.setFamilyMedicalHistory(rs.getString("FamilyMedicalHistory"));
                currentDossier.setInsuranceProvider(rs.getString("InsuranceProvider"));
                currentDossier.setInsurancePolicyNumber(rs.getString("InsurancePolicyNumber"));

                // Populate form fields with data
                bloodTypeComboBox.setValue(currentDossier.getBloodType());
                allergiesField.setText(currentDossier.getAllergies());
                currentMedicationsField.setText(currentDossier.getCurrentMedications());
                bloodPressureField.setText(currentDossier.getBloodPressure());
                chronicConditionsField.setText(currentDossier.getChronicConditions());
                previousSurgeriesField.setText(currentDossier.getPreviousSurgeries());
                familyMedicalHistoryField.setText(currentDossier.getFamilyMedicalHistory());

                // Format and set date fields
                if (currentDossier.getDateCreated() != null) {
                    dateCreatedField.setText(dateFormat.format(currentDossier.getDateCreated()));
                }
                if (currentDossier.getLastUpdated() != null) {
                    lastUpdatedField.setText(dateFormat.format(currentDossier.getLastUpdated()));
                }

                insuranceProviderField.setText(currentDossier.getInsuranceProvider());
                insurancePolicyNumberField.setText(currentDossier.getInsurancePolicyNumber());

                PatientDataHolder.getInstance().setCurrentDossier(currentDossier);
            } else {
                // No dossier exists yet
                currentDossier = new DossierPatient();
                currentDossier.setPatientID(currentPatient.getID());

                // Set current date/time for creation fields
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                dateCreatedField.setText(dateFormat.format(now));
                lastUpdatedField.setText(dateFormat.format(now));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyRoleBasedAccess() {
        String userRole = UserSession.getInstance().getRole();

        if ("personnel".equals(userRole)) {
            // For personnel role: only show dateCreated, lastUpdated, insuranceProvider, and insurancePolicyNumber
            // Hide other medical information
            medicalInfoGrid.setVisible(false);
            medicalInfoGrid.setManaged(false);
            additionalInfoGrid.setVisible(false);
            additionalInfoGrid.setManaged(false);

            // Keep dates and insurance information visible
            datesAndInsuranceGrid.setVisible(true);

            // Date fields are read-only (already set in FXML)
            // Insurance fields should be editable
            insuranceProviderField.setEditable(true);
            insurancePolicyNumberField.setEditable(true);

            // Save button should be visible to allow updating insurance info
            saveButton.setVisible(true);
        }
    }

    @FXML
    public void saveDossier(ActionEvent event) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String userRole = UserSession.getInstance().getRole();

        try {
            if ("personnel".equals(userRole)) {
                // For personnel role: only update insurance information
                if (isNewDossier) {
                    // Insert new record with minimal information
                    String insertQuery = "INSERT INTO dossierpatient (PatientID, DateCreated, LastUpdated, " +
                            "InsuranceProvider, InsurancePolicyNumber) " +
                            "VALUES (?, ?, ?, ?, ?)";

                    PreparedStatement pstmt = conn.prepareStatement(insertQuery);
                    pstmt.setString(1, currentPatient.getID());
                    pstmt.setTimestamp(2, now);
                    pstmt.setTimestamp(3, now);
                    pstmt.setString(4, insuranceProviderField.getText());
                    pstmt.setString(5, insurancePolicyNumberField.getText());

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Informations d'assurance enregistrées avec succès");
                        returnAction(event);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'enregistrement des informations d'assurance");
                    }
                } else {
                    // Update only insurance information
                    String updateQuery = "UPDATE dossierpatient SET LastUpdated=?, " +
                            "InsuranceProvider=?, InsurancePolicyNumber=? " +
                            "WHERE PatientID=?";

                    PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                    pstmt.setTimestamp(1, now);
                    pstmt.setString(2, insuranceProviderField.getText());
                    pstmt.setString(3, insurancePolicyNumberField.getText());
                    pstmt.setString(4, currentPatient.getID());

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Informations d'assurance mises à jour avec succès");
                        lastUpdatedField.setText(dateFormat.format(now));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la mise à jour des informations d'assurance");
                    }
                }
            } else {
                // For doctors or other roles: update all information
                if (isNewDossier) {
                    // Insert new record
                    String insertQuery = "INSERT INTO dossierpatient (PatientID, BloodType, Allergies, CurrentMedications, " +
                            "DateCreated, LastUpdated, BloodPressure, ChronicConditions, PreviousSurgeries, " +
                            "FamilyMedicalHistory, InsuranceProvider, InsurancePolicyNumber) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    PreparedStatement pstmt = conn.prepareStatement(insertQuery);
                    pstmt.setString(1, currentPatient.getID());
                    pstmt.setString(2, bloodTypeComboBox.getValue());
                    pstmt.setString(3, allergiesField.getText());
                    pstmt.setString(4, currentMedicationsField.getText());
                    pstmt.setTimestamp(5, now);
                    pstmt.setTimestamp(6, now);
                    pstmt.setString(7, bloodPressureField.getText());
                    pstmt.setString(8, chronicConditionsField.getText());
                    pstmt.setString(9, previousSurgeriesField.getText());
                    pstmt.setString(10, familyMedicalHistoryField.getText());
                    pstmt.setString(11, insuranceProviderField.getText());
                    pstmt.setString(12, insurancePolicyNumberField.getText());

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Dossier patient créé avec succès");
                        returnAction(event);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la création du dossier patient");
                    }
                } else {
                    // Update existing record
                    String updateQuery = "UPDATE dossierpatient SET BloodType=?, Allergies=?, CurrentMedications=?, " +
                            "LastUpdated=?, BloodPressure=?, ChronicConditions=?, PreviousSurgeries=?, " +
                            "FamilyMedicalHistory=?, InsuranceProvider=?, InsurancePolicyNumber=? " +
                            "WHERE PatientID=?";

                    PreparedStatement pstmt = conn.prepareStatement(updateQuery);
                    pstmt.setString(1, bloodTypeComboBox.getValue());
                    pstmt.setString(2, allergiesField.getText());
                    pstmt.setString(3, currentMedicationsField.getText());
                    pstmt.setTimestamp(4, now);
                    pstmt.setString(5, bloodPressureField.getText());
                    pstmt.setString(6, chronicConditionsField.getText());
                    pstmt.setString(7, previousSurgeriesField.getText());
                    pstmt.setString(8, familyMedicalHistoryField.getText());
                    pstmt.setString(9, insuranceProviderField.getText());
                    pstmt.setString(10, insurancePolicyNumberField.getText());
                    pstmt.setString(11, currentPatient.getID());

                    int result = pstmt.executeUpdate();
                    if (result > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Dossier patient mis à jour avec succès");
                        lastUpdatedField.setText(dateFormat.format(now));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la mise à jour du dossier patient");
                    }
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de base de données", e.getMessage());
            e.printStackTrace();
        }
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
        // Close the current stage/dialog instead of switching scenes
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}