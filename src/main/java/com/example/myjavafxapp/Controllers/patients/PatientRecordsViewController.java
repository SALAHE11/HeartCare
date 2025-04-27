package com.example.myjavafxapp.Controllers.patients;

import com.example.myjavafxapp.Controllers.appointments.AppointmentFormController;
import com.example.myjavafxapp.Models.appointment.Appointment;
import com.example.myjavafxapp.Models.appointment.AppointmentManager;
import com.example.myjavafxapp.Models.patient.Patient;
import com.example.myjavafxapp.Models.patient.PatientDataHolder;
import com.example.myjavafxapp.Models.patient.PatientManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class PatientRecordsViewController implements Initializable {
    @FXML private TextField patientSearchField;
    @FXML private Button searchButton;
    @FXML private Button addPatientButton;
    @FXML private Button backToCalendarButton;
    @FXML private ListView<Patient> patientList;
    @FXML private GridPane patientDetailsPane;

    // Patient detail labels
    @FXML private Label patientIdLabel;
    @FXML private Label patientNameLabel;
    @FXML private Label patientBirthdateLabel;
    @FXML private Label patientSexLabel;
    @FXML private Label patientAddressLabel;
    @FXML private Label patientPhoneLabel;
    @FXML private Label patientEmailLabel;

    @FXML private Button editPatientButton;
    @FXML private Button viewMedicalRecordButton;
    @FXML private Button newAppointmentButton;
    @FXML private TableView<Appointment> appointmentHistoryTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> reasonColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, Void> actionsColumn;

    private PatientManager patientManager = PatientManager.getInstance();
    private AppointmentManager appointmentManager = AppointmentManager.getInstance();
    private ObservableList<Patient> patientsData = FXCollections.observableArrayList();
    private ObservableList<Appointment> appointmentsData = FXCollections.observableArrayList();
    private Patient currentPatient;

    // Formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupPatientList();
        setupAppointmentHistory();

        // Initially hide the patient details
        patientDetailsPane.setVisible(false);
        editPatientButton.setDisable(true);
        viewMedicalRecordButton.setDisable(true);
        newAppointmentButton.setDisable(true);

        // Load initial patient list (limited to first 100 for performance)
        loadInitialPatients();
    }

    /**
     * Set up the patient list view
     */
    private void setupPatientList() {
        patientList.setItems(patientsData);

        // Set the cell factory to display patient names
        patientList.setCellFactory(lv -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                if (empty || patient == null) {
                    setText(null);
                } else {
                    setText(patient.getFNAME() + " " + patient.getLNAME());
                }
            }
        });

        // Add selection listener
        patientList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentPatient = newVal;
                displayPatientDetails(newVal);
                loadPatientAppointments(newVal);
                editPatientButton.setDisable(false);
                viewMedicalRecordButton.setDisable(false);
                newAppointmentButton.setDisable(false);
            } else {
                patientDetailsPane.setVisible(false);
                editPatientButton.setDisable(true);
                viewMedicalRecordButton.setDisable(true);
                newAppointmentButton.setDisable(true);
            }
        });
    }

    /**
     * Set up the appointment history table
     */
    private void setupAppointmentHistory() {
        // Set up the table columns
        dateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getAppointmentDateTime().toLocalDate();
            return new SimpleStringProperty(date.format(dateFormatter));
        });

        timeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getAppointmentDateTime().format(timeFormatter));
        });

        doctorColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getDoctorName());
        });

        reasonColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getReasonForVisit());
        });

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            // Translate appointment status for display
            switch (status) {
                case "Scheduled": return new SimpleStringProperty("Programmé");
                case "CheckedIn": return new SimpleStringProperty("Enregistré");
                case "InProgress": return new SimpleStringProperty("En cours");
                case "Completed": return new SimpleStringProperty("Terminé");
                case "Missed": return new SimpleStringProperty("Manqué");
                case "Patient_Cancelled": return new SimpleStringProperty("Annulé (Patient)");
                case "Clinic_Cancelled": return new SimpleStringProperty("Annulé (Clinique)");
                default: return new SimpleStringProperty(status);
            }
        });

        // Set up the actions column
        actionsColumn.setCellFactory(col -> new TableCell<Appointment, Void>() {
            private final Button viewButton = new Button();

            {
                // Set up view button
                FontIcon viewIcon = new FontIcon("fas-eye");
                viewIcon.setIconSize(14);
                viewButton.setGraphic(viewIcon);
                viewButton.getStyleClass().add("icon-button");

                viewButton.setOnAction(e -> {
                    Appointment appointment = getTableView().getItems().get(getIndex());
                    viewAppointment(appointment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewButton);
                    setGraphic(buttons);
                }
            }
        });

        // Set the items
        appointmentHistoryTable.setItems(appointmentsData);

        // Add row style based on appointment status
        appointmentHistoryTable.setRowFactory(tv -> new TableRow<Appointment>() {
            @Override
            protected void updateItem(Appointment appointment, boolean empty) {
                super.updateItem(appointment, empty);

                if (appointment == null || empty) {
                    setStyle("");
                } else {
                    // Apply style class based on status
                    getStyleClass().removeAll(
                            "status-scheduled", "status-completed", "status-missed",
                            "status-cancelled", "status-checked-in", "status-in-progress"
                    );

                    if ("Scheduled".equals(appointment.getStatus())) {
                        getStyleClass().add("status-scheduled");
                    } else if ("Completed".equals(appointment.getStatus())) {
                        getStyleClass().add("status-completed");
                    } else if ("Missed".equals(appointment.getStatus())) {
                        getStyleClass().add("status-missed");
                    } else if ("Patient_Cancelled".equals(appointment.getStatus()) ||
                            "Clinic_Cancelled".equals(appointment.getStatus())) {
                        getStyleClass().add("status-cancelled");
                    } else if ("CheckedIn".equals(appointment.getStatus())) {
                        getStyleClass().add("status-checked-in");
                    } else if ("InProgress".equals(appointment.getStatus())) {
                        getStyleClass().add("status-in-progress");
                    }
                }
            }
        });
    }

    /**
     * Load initial list of patients
     */
    private void loadInitialPatients() {
        // For performance, limit to a reasonable number
        List<Patient> patients = patientManager.getAllPatients();
        patientsData.setAll(patients);
    }

    /**
     * Display patient details
     */
    private void displayPatientDetails(Patient patient) {
        // Set the label values
        patientIdLabel.setText(patient.getID());
        patientNameLabel.setText(patient.getFNAME() + " " + patient.getLNAME());
        patientBirthdateLabel.setText(patient.getBIRTHDATE());
        patientSexLabel.setText(patient.getSEXE());
        patientAddressLabel.setText(patient.getADRESSE());
        patientPhoneLabel.setText(String.valueOf(patient.getTELEPHONE()));
        patientEmailLabel.setText(patient.getEMAIL());

        // Show the details pane
        patientDetailsPane.setVisible(true);
    }

    /**
     * Load patient appointments
     */
    private void loadPatientAppointments(Patient patient) {
        // Clear existing data
        appointmentsData.clear();

        // Get appointments for this patient
        List<Appointment> appointments = patientManager.getPatientAppointmentHistory(patient.getID());
        appointmentsData.addAll(appointments);
    }

    /**
     * View appointment details
     */
    private void viewAppointment(Appointment appointment) {
        try {
            // Load the appointment form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/appointments/AppointmentForm.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Voir Rendez-vous");
            dialog.setScene(new Scene(loader.load()));

            // Set the appointment in the controller
            AppointmentFormController controller = loader.getController();
            controller.setAppointment(appointment);

            dialog.showAndWait();

            // Refresh the appointment history after dialog closes
            if (currentPatient != null) {
                loadPatientAppointments(currentPatient);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la vue du rendez-vous : " + e.getMessage());
        }
    }

    /**
     * Handle search button click
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        String searchTerm = patientSearchField.getText().trim();

        if (searchTerm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur de recherche", "Veuillez entrer un terme de recherche");
            return;
        }

        // Search for patients
        List<Patient> results = patientManager.searchPatients(searchTerm);

        if (results.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Aucun résultat", "Aucun patient trouvé correspondant à '" + searchTerm + "'");
            return;
        }

        // Update the patient list
        patientsData.setAll(results);
    }

    /**
     * Handle add patient button click
     */
    @FXML
    private void handleAddPatient(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/patients/ajouterPatient.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Ajouter un nouveau patient");
            dialog.setScene(new Scene(loader.load()));

            dialog.showAndWait();

            // Refresh patient list after dialog closes
            loadInitialPatients();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire d'ajout de patient : " + e.getMessage());
        }
    }

    /**
     * Handle edit patient button click
     */
    @FXML
    private void handleEditPatient(ActionEvent event) {
        if (currentPatient == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de sélection", "Veuillez sélectionner un patient d'abord");
            return;
        }

        try {
            // Store the selected patient in the data holder for access in the edit screen
            PatientDataHolder.getInstance().setCurrentPatient(currentPatient);

            // Load edit patient view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/patients/editerPatient.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Modifier le patient");
            dialog.setScene(new Scene(loader.load()));

            dialog.showAndWait();

            // Refresh the patient list and details after dialog closes
            loadInitialPatients();

            // Refresh the currently displayed patient details if it still exists
            if (currentPatient != null) {
                // Retrieve the updated patient data
                Patient updatedPatient = patientManager.getPatientById(currentPatient.getID());
                if (updatedPatient != null) {
                    currentPatient = updatedPatient;
                    displayPatientDetails(updatedPatient);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification de patient : " + e.getMessage());
        }
    }

    /**
     * Handle view medical record button click
     */
    @FXML
    private void handleViewMedicalRecord(ActionEvent event) {
        if (currentPatient == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de sélection", "Veuillez sélectionner un patient d'abord");
            return;
        }

        try {
            // Store the selected patient in the data holder for access in the next screen
            PatientDataHolder.getInstance().setCurrentPatient(currentPatient);

            // Load dossier patient view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/patients/dossierPatient.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Dossier médical du patient");
            dialog.setScene(new Scene(loader.load()));

            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le dossier médical : " + e.getMessage());
        }
    }

    /**
     * Handle new appointment button click
     */
    @FXML
    private void handleNewAppointment(ActionEvent event) {
        if (currentPatient == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de sélection", "Veuillez sélectionner un patient d'abord");
            return;
        }

        try {
            // Create a new appointment for this patient but don't set rendezVousID
            // This ensures it's treated as a new appointment, not an edit
            Appointment newAppointment = new Appointment();
            newAppointment.setPatientID(currentPatient.getID());
            newAppointment.setPatientName(currentPatient.getFNAME() + " " + currentPatient.getLNAME());
            // Important: ensure the appointment is correctly flagged as new
            newAppointment.setRendezVousID(0); // Use 0 to indicate it's a new appointment

            // Load the appointment form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/appointments/AppointmentForm.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Nouveau Rendez-vous");
            dialog.setScene(new Scene(loader.load()));

            // Set the appointment in the controller with a flag indicating it's new
            AppointmentFormController controller = loader.getController();
            controller.setAppointment(newAppointment);
            controller.setNewAppointment(true); // Add this flag to the controller

            dialog.showAndWait();

            // Refresh the appointment history after dialog closes
            loadPatientAppointments(currentPatient);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de rendez-vous : " + e.getMessage());
        }
    }

    /**
     * Handle back to calendar button click
     */
    @FXML
    private void handleBackToCalendar(ActionEvent event) {
        try {
            // Load the calendar view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/appointments/CalendarView.fxml"));

            // Get the current stage
            Stage currentStage = (Stage) backToCalendarButton.getScene().getWindow();

            // Set the new scene
            currentStage.setScene(new Scene(loader.load()));

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la vue du calendrier : " + e.getMessage());
        }
    }

    /**
     * Show an alert dialog
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}