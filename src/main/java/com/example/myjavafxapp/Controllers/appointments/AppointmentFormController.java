package com.example.myjavafxapp.Controllers.appointments;

import com.example.myjavafxapp.Models.appointment.Appointment;
import com.example.myjavafxapp.Models.appointment.AppointmentManager;
import com.example.myjavafxapp.Models.patient.Patient;
import com.example.myjavafxapp.Models.patient.PatientManager;
import com.example.myjavafxapp.Models.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AppointmentFormController implements Initializable {
    @FXML private Label formTitleLabel;
    @FXML private TextField patientSearchField;
    @FXML private Button searchButton;
    @FXML private ComboBox<Patient> patientSelector;
    @FXML private ComboBox<User> doctorSelector;
    @FXML private DatePicker appointmentDate;
    @FXML private ComboBox<LocalTime> appointmentTime;
    @FXML private TextField reasonField;
    @FXML private ComboBox<String> prioritySelector;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> statusSelector;
    @FXML private TextArea notesField;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private Appointment currentAppointment;
    private boolean isEditMode = false;
    private AppointmentManager appointmentManager = AppointmentManager.getInstance();
    private PatientManager patientManager = PatientManager.getInstance();

    // Time formatter
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Setup dropdown selectors
        setupPriorityDropdown();
        setupStatusDropdown();
        setupTimeDropdown();
        loadDoctors();
        setupPatientSelector();

        // Set default date to today
        appointmentDate.setValue(LocalDate.now());

        // Add listener to date change to update available times
        appointmentDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateAvailableTimes();
        });

        // Add listener to doctor change to update available times
        doctorSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateAvailableTimes();
        });
    }

    private boolean isNewAppointment = false;

    // Ajoutez cette nouvelle méthode à la classe AppointmentFormController
    public void setNewAppointment(boolean isNew) {
        this.isNewAppointment = isNew;
        // Mettre à jour le titre du formulaire
        if (isNew) {
            formTitleLabel.setText("Nouveau Rendez-vous");
        } else {
            formTitleLabel.setText("Modifier Rendez-vous");
        }
    }

    // Modifiez la méthode setAppointment pour reconnaître correctement les nouveaux rendez-vous
    public void setAppointment(Appointment appointment) {
        if (appointment != null) {
            // Déterminer si c'est un nouveau rendez-vous ou une édition
            isNewAppointment = (appointment.getRendezVousID() == 0);
            currentAppointment = appointment;

            // Mettre à jour le titre du formulaire
            if (isNewAppointment) {
                formTitleLabel.setText("Nouveau Rendez-vous");
            } else {
                formTitleLabel.setText("Modifier Rendez-vous");
            }

            // Charger les données du rendez-vous dans le formulaire
            populateForm();

            // Afficher ou masquer le champ de statut en fonction du mode
            statusLabel.setVisible(!isNewAppointment);
            statusSelector.setVisible(!isNewAppointment);
        } else {
            // Nouveau rendez-vous par défaut
            isNewAppointment = true;
            currentAppointment = new Appointment();
            formTitleLabel.setText("Nouveau Rendez-vous");

            // Définir les valeurs par défaut
            appointmentDate.setValue(LocalDate.now());
            prioritySelector.getSelectionModel().select("Normal");

            // Masquer le champ de statut pour les nouveaux rendez-vous
            statusLabel.setVisible(false);
            statusSelector.setVisible(false);
        }
    }

    /**
     * Populate form with appointment data
     */
    private void populateForm() {
        // Set patient
        if (currentAppointment.getPatientID() != null) {
            Patient patient = patientManager.getPatientById(currentAppointment.getPatientID());
            if (patient != null) {
                patientSelector.setValue(patient);
            }
        }

        // Set doctor
        if (currentAppointment.getMedicinID() != null) {
            for (User doctor : doctorSelector.getItems()) {
                if (doctor.getId().equals(currentAppointment.getMedicinID())) {
                    doctorSelector.setValue(doctor);
                    break;
                }
            }
        }

        // Set date and time
        if (currentAppointment.getAppointmentDateTime() != null) {
            appointmentDate.setValue(currentAppointment.getAppointmentDateTime().toLocalDate());

            // Need to add the time to the dropdown if it doesn't exist yet
            LocalTime appointmentTimeValue = currentAppointment.getAppointmentDateTime().toLocalTime();
            boolean timeExists = false;

            for (LocalTime time : appointmentTime.getItems()) {
                if (time.equals(appointmentTimeValue)) {
                    timeExists = true;
                    break;
                }
            }

            if (!timeExists) {
                appointmentTime.getItems().add(appointmentTimeValue);
                FXCollections.sort(appointmentTime.getItems());
            }

            appointmentTime.setValue(appointmentTimeValue);
        }

        // Set reason
        reasonField.setText(currentAppointment.getReasonForVisit());

        // Set priority
        prioritySelector.setValue(currentAppointment.getPriority());

        // Set status
        statusSelector.setValue(currentAppointment.getStatus());

        // Set notes
        notesField.setText(currentAppointment.getNotes());
    }

    /**
     * Set up priority dropdown
     */
    private void setupPriorityDropdown() {
        ObservableList<String> priorities = FXCollections.observableArrayList("Normal", "Urgent");
        prioritySelector.setItems(priorities);
        prioritySelector.setValue("Normal");
    }

    /**
     * Set up status dropdown
     */
    private void setupStatusDropdown() {
        ObservableList<String> statuses = FXCollections.observableArrayList(
                "Scheduled", "CheckedIn", "InProgress", "Completed", "Missed",
                "Rescheduled", "Patient_Cancelled", "Clinic_Cancelled"
        );
        statusSelector.setItems(statuses);
    }

    /**
     * Set up time dropdown with 15-minute increments (changed from 30-minute)
     */
    private void setupTimeDropdown() {
        ObservableList<LocalTime> times = FXCollections.observableArrayList();

        // Add times from 8:00 to 17:45 in 15-minute increments (changed from 30-minute)
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 45);

        LocalTime current = startTime;
        while (!current.isAfter(endTime)) {
            times.add(current);
            current = current.plusMinutes(15); // Changed from 30 to 15 minutes
        }

        appointmentTime.setItems(times);

        // Set custom cell factory to format the time
        appointmentTime.setCellFactory(lv -> new ListCell<LocalTime>() {
            @Override
            protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(time.format(timeFormatter));
                }
            }
        });

        // Set converter for the selected item display
        appointmentTime.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime time) {
                if (time == null) {
                    return null;
                }
                return time.format(timeFormatter);
            }

            @Override
            public LocalTime fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                return LocalTime.parse(string, timeFormatter);
            }
        });
    }

    /**
     * Load doctors from database
     */
    private void loadDoctors() {
        List<User> doctors = appointmentManager.getAllDoctors();
        doctorSelector.setItems(FXCollections.observableArrayList(doctors));

        // Set custom cell factory
        doctorSelector.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText("Dr. " + user.getLastName() + ", " + user.getFirstName());
                }
            }
        });

        // Set converter for the selected item display
        doctorSelector.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                if (user == null) {
                    return null;
                }
                return "Dr. " + user.getLastName() + ", " + user.getFirstName();
            }

            @Override
            public User fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
    }

    /**
     * Set up patient selector and search functionality
     */
    private void setupPatientSelector() {
        // Set custom cell factory
        patientSelector.setCellFactory(lv -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                if (empty || patient == null) {
                    setText(null);
                } else {
                    setText(patient.getFNAME() + " " + patient.getLNAME() + " (" + patient.getID() + ")");
                }
            }
        });

        // Set converter for the selected item display
        patientSelector.setConverter(new StringConverter<Patient>() {
            @Override
            public String toString(Patient patient) {
                if (patient == null) {
                    return null;
                }
                return patient.getFNAME() + " " + patient.getLNAME() + " (" + patient.getID() + ")";
            }

            @Override
            public Patient fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
    }

    /**
     * Update available times based on selected doctor and date
     */
    private void updateAvailableTimes() {
        // Get selected doctor and date
        User selectedDoctor = doctorSelector.getValue();
        LocalDate selectedDate = appointmentDate.getValue();

        if (selectedDoctor == null || selectedDate == null) {
            return;
        }

        // Save currently selected time
        LocalTime currentlySelectedTime = appointmentTime.getValue();

        // Get available time slots
        List<LocalDateTime> availableSlots = appointmentManager.getAvailableTimeSlots(
                selectedDoctor.getId(), selectedDate);

        // Extract just the time portion
        ObservableList<LocalTime> availableTimes = FXCollections.observableArrayList();
        for (LocalDateTime slot : availableSlots) {
            availableTimes.add(slot.toLocalTime());
        }

        // If this is edit mode, add the current appointment time to available times
        if (isEditMode && currentAppointment.getAppointmentDateTime() != null) {
            LocalTime appointmentTimeValue = currentAppointment.getAppointmentDateTime().toLocalTime();
            if (!availableTimes.contains(appointmentTimeValue)) {
                availableTimes.add(appointmentTimeValue);
                FXCollections.sort(availableTimes);
            }
        }

        // Update the time dropdown
        appointmentTime.setItems(availableTimes);

        // Try to restore previous selection if it's still available
        if (currentlySelectedTime != null && availableTimes.contains(currentlySelectedTime)) {
            appointmentTime.setValue(currentlySelectedTime);
        } else if (!availableTimes.isEmpty()) {
            appointmentTime.setValue(availableTimes.get(0));
        }
    }

    /**
     * Handle patient search button click
     */
    @FXML
    private void handlePatientSearch(ActionEvent event) {
        // Get search term
        String searchTerm = patientSearchField.getText().trim();

        if (searchTerm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Search Error", "Please enter a search term");
            return;
        }

        // Search for patients
        List<Patient> results = patientManager.searchPatients(searchTerm);

        if (results.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No patients found matching '" + searchTerm + "'");
            return;
        }

        // Update patient selector with results
        patientSelector.setItems(FXCollections.observableArrayList(results));
        patientSelector.show(); // Show the dropdown
    }

    /**
     * Handle save button click
     */
    @FXML
    private void handleSave(ActionEvent event) {
        if (validateForm()) {
            try {
                // Update the appointment object with form data
                updateAppointmentFromForm();

                boolean success;

                // Check if this is a new urgent appointment
                if (isNewAppointment && "Urgent".equals(currentAppointment.getPriority())) {
                    // Use the special urgent scheduling method
                    success = appointmentManager.scheduleUrgentAppointment(currentAppointment);
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès",
                                "Rendez-vous urgent créé avec succès et les autres rendez-vous ont été décalés si nécessaire");
                        closeForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la création du rendez-vous urgent");
                    }
                } else {
                    // Normal appointment handling (unchanged)
                    if (isNewAppointment) {
                        // Create a new appointment
                        success = appointmentManager.createAppointment(currentAppointment);
                        if (success) {
                            showAlert(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous créé avec succès");
                            closeForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la création du rendez-vous");
                        }
                    } else {
                        // Update an existing appointment
                        success = appointmentManager.updateAppointment(currentAppointment);
                        if (success) {
                            showAlert(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous mis à jour avec succès");
                            closeForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour du rendez-vous");
                        }
                    }
                }

                // Refresh the calendar view if possible
                Stage stage = (Stage) cancelButton.getScene().getWindow();
                if (stage.getUserData() instanceof CalendarViewController) {
                    CalendarViewController controller = (CalendarViewController) stage.getUserData();
                    controller.updateCalendarView();
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        closeForm();
    }

    /**
     * Close the form window
     */
    private void closeForm() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Validate form fields
     */
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        // Check required fields
        if (patientSelector.getValue() == null) {
            errorMessage.append("Please select a patient\n");
        }

        if (doctorSelector.getValue() == null) {
            errorMessage.append("Please select a doctor\n");
        }

        if (appointmentDate.getValue() == null) {
            errorMessage.append("Please select a date\n");
        }

        if (appointmentTime.getValue() == null) {
            errorMessage.append("Please select a time\n");
        }

        if (reasonField.getText().trim().isEmpty()) {
            errorMessage.append("Please enter a reason for the visit\n");
        }

        if (prioritySelector.getValue() == null) {
            errorMessage.append("Please select a priority\n");
        }

        if (isEditMode && statusSelector.getValue() == null) {
            errorMessage.append("Please select a status\n");
        }

        // Check if date is in the past
        if (appointmentDate.getValue() != null &&
                appointmentDate.getValue().isBefore(LocalDate.now())) {
            errorMessage.append("Cannot schedule appointments in the past\n");
        }

        // Show alert if any errors
        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", errorMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * Update appointment object from form data
     */
    private void updateAppointmentFromForm() {
        // Get selected patient
        Patient selectedPatient = patientSelector.getValue();
        currentAppointment.setPatientID(selectedPatient.getID());
        currentAppointment.setPatientName(selectedPatient.getFNAME() + " " + selectedPatient.getLNAME());

        // Get selected doctor
        User selectedDoctor = doctorSelector.getValue();
        currentAppointment.setMedicinID(selectedDoctor.getId());
        currentAppointment.setDoctorName(selectedDoctor.getFullName());

        // Get date and time
        LocalDate date = appointmentDate.getValue();
        LocalTime time = appointmentTime.getValue();
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        currentAppointment.setAppointmentDateTime(dateTime);

        // Get reason
        currentAppointment.setReasonForVisit(reasonField.getText().trim());

        // Get priority
        currentAppointment.setPriority(prioritySelector.getValue());

        // Get status (if in edit mode)
        if (isEditMode) {
            currentAppointment.setStatus(statusSelector.getValue());
        } else {
            currentAppointment.setStatus("Scheduled");
            currentAppointment.setStatusReason("Initial creation");
        }

        // Get notes
        currentAppointment.setNotes(notesField.getText());

        // Ensure no null values
        if (currentAppointment.getStatusReason() == null) {
            currentAppointment.setStatusReason("");
        }

        if (currentAppointment.getNotes() == null) {
            currentAppointment.setNotes("");
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