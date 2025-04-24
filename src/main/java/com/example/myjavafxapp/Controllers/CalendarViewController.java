package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CalendarViewController implements Initializable {
    @FXML private Button prevDayBtn;
    @FXML private Button todayBtn;
    @FXML private Button nextDayBtn;
    @FXML private Label currentDateLabel;
    @FXML private ComboBox<User> doctorFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button newAppointmentBtn;
    @FXML private GridPane calendarGrid;

    // Navigation buttons
    @FXML private Button homeButton;
    @FXML private Button gestionRendezVous;
    @FXML private Button gestionPaiment;
    @FXML private Button dossierPatient;
    @FXML private Button statistiqueGlobales;
    @FXML private Button rapportQuotidien;
    @FXML private Button sauvegarde;
    @FXML private Button gestionUtilisateur;
    @FXML private Button logoutButton;
    @FXML private HBox navButtonsContainer;

    // Doctor column headers
    @FXML private Label doctor1Label;
    @FXML private Label doctor2Label;
    @FXML private Label doctor3Label;
    @FXML private Label doctor4Label;

    // Statistics labels
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label completedAppointmentsLabel;
    @FXML private Label inProgressAppointmentsLabel;
    @FXML private Label checkedInAppointmentsLabel;
    @FXML private Label scheduledAppointmentsLabel;
    @FXML private Label missedAppointmentsLabel;
    @FXML private Label cancelledAppointmentsLabel; // Added new label for cancelled appointments

    @FXML private VBox upcomingAppointmentsBox;

    private LocalDate currentDate = LocalDate.now();
    private AppointmentManager appointmentManager = AppointmentManager.getInstance();
    private List<User> doctors;
    private Map<String, Integer> doctorColumnMap = new HashMap<>();
    private Timer refreshTimer;

    // Formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configure icon visibility based on user role
        configureIconVisibility();

        // Load doctors for the filter
        loadDoctorsForFilter();

        // Setup status filter
        setupStatusFilter();

        // Initialize the calendar grid with time slots
        initializeCalendarGrid();

        // Set current date and load appointments
        updateDateLabel();
        updateCalendarView();

        // Setup auto-refresh (every 5 minutes)
        setupAutoRefresh();
    }

    /**
     * Configure the visibility of icons based on user role
     */
    private void configureIconVisibility() {
        // Get user role from the session
        UserSession userSession = UserSession.getInstance();
        String userRole = userSession.getRole();

        if (userRole == null) {
            // If role is not set, default to showing only basic icons
            setBasicIconsVisibility();
            return;
        }

        switch (userRole.toLowerCase()) {
            case "admin":
                // For admin, show all icons
                setAllIconsVisibility(true);
                break;
            case "medecin":
            case "personnel":
                // For medecin and personnel, show only the basic icons
                setBasicIconsVisibility();
                break;
            default:
                // For unknown roles, show only basic icons
                setBasicIconsVisibility();
                break;
        }
    }

    /**
     * Set visibility for basic icons (home, payment, patient folder)
     * and hide administrative icons
     */
    private void setBasicIconsVisibility() {
        // Clear all existing buttons
        navButtonsContainer.getChildren().clear();

        // Add only the 3 basic icons in the correct order
        navButtonsContainer.getChildren().add(homeButton);
        navButtonsContainer.getChildren().add(gestionPaiment);
        navButtonsContainer.getChildren().add(dossierPatient);
    }

    /**
     * Set visibility for all icons
     * @param visible true to show all icons, false to hide all icons
     */
    private void setAllIconsVisibility(boolean visible) {
        if (visible) {
            // Clear all existing buttons
            navButtonsContainer.getChildren().clear();

            // Add all icons to the container in the correct order
            // Removed gestionRendezVous (calendar) since it's redundant with the home button
            navButtonsContainer.getChildren().addAll(
                    homeButton,
                    gestionPaiment,
                    dossierPatient,
                    statistiqueGlobales,
                    rapportQuotidien,
                    sauvegarde,
                    gestionUtilisateur
            );
        } else {
            navButtonsContainer.getChildren().clear();
        }
    }

    private void loadDoctorsForFilter() {
        doctors = appointmentManager.getAllDoctors();

        // Add a "All Doctors" option at the beginning
        User allDoctors = new User();
        allDoctors.setFirstName("Tous");
        allDoctors.setLastName("les Médecins");

        List<User> doctorFilterList = new ArrayList<>();
        doctorFilterList.add(allDoctors);
        doctorFilterList.addAll(doctors);

        ObservableList<User> doctorItems = FXCollections.observableArrayList(doctorFilterList);
        doctorFilter.setItems(doctorItems);
        doctorFilter.getSelectionModel().selectFirst();

        // Add listener to filter
        doctorFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateCalendarView();
        });

        // Set up doctor column headers
        setupDoctorHeaders();
    }

    private void setupStatusFilter() {
        ObservableList<String> statusItems = FXCollections.observableArrayList(
                "Tous les statuts",
                "Programmé",
                "Enregistré",
                "En cours",
                "Terminé",
                "Manqué",
                "Annulé"
        );

        statusFilter.setItems(statusItems);
        statusFilter.getSelectionModel().selectFirst();

        // Add listener to filter
        statusFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateCalendarView();
        });
    }

    private void setupDoctorHeaders() {
        // Make sure we have doctor headers
        if (doctor1Label == null || doctor2Label == null || doctor3Label == null || doctor4Label == null) {
            return;
        }

        Label[] doctorLabels = {doctor1Label, doctor2Label, doctor3Label, doctor4Label};

        // Reset the doctor column mapping
        doctorColumnMap.clear();

        // Get up to 4 doctors to display in columns
        int numCols = Math.min(doctors.size(), 4);

        for (int i = 0; i < numCols; i++) {
            User doctor = doctors.get(i);
            doctorLabels[i].setText("Dr. " + doctor.getLastName());
            doctorColumnMap.put(doctor.getId(), i + 1); // +1 because column 0 is the time column
        }

        // Hide unused columns
        for (int i = numCols; i < 4; i++) {
            doctorLabels[i].setText("");
            doctorLabels[i].setVisible(false);  // Hide the label completely
        }
    }

    private void initializeCalendarGrid() {
        // Reset grid
        calendarGrid.getRowConstraints().clear();
        if (calendarGrid.getRowConstraints().size() <= 1) {
            // Keep the header row
            RowConstraints headerRow = new RowConstraints(30);
            calendarGrid.getRowConstraints().add(headerRow);

            // Add time slots (every 15 minutes from 8:00 to 17:45)
            LocalTime startTime = LocalTime.of(8, 0);
            LocalTime endTime = LocalTime.of(17, 45);

            int rowIndex = 1;
            LocalTime currentTime = startTime;

            while (!currentTime.isAfter(endTime)) {
                // Add row constraint
                RowConstraints rowConstraint = new RowConstraints(40); // Reduced from 60px to 40px due to more slots
                calendarGrid.getRowConstraints().add(rowConstraint);

                // Add time label
                Label timeLabel = new Label(currentTime.format(timeFormatter));
                timeLabel.getStyleClass().add("time-label");
                calendarGrid.add(timeLabel, 0, rowIndex);

                // Add empty panes for each doctor column
                for (int col = 1; col <= 4; col++) {
                    StackPane cellPane = new StackPane();
                    cellPane.getStyleClass().add("time-slot");

                    // Store time info in the pane's properties for later use
                    cellPane.getProperties().put("time", currentTime);
                    cellPane.getProperties().put("column", col);

                    // Add click handler to create appointment
                    // Create a final copy of the current time for use in the lambda
                    final LocalTime timeForLambda = currentTime;
                    int doctorColumn = col;
                    cellPane.setOnMouseClicked(e -> handleTimeSlotClick(e, doctorColumn, timeForLambda));

                    calendarGrid.add(cellPane, col, rowIndex);
                }

                // Increment time by 15 minutes instead of 30
                currentTime = currentTime.plusMinutes(15);
                rowIndex++;
            }
        }
    }

    private void handleTimeSlotClick(MouseEvent event, int doctorColumn, LocalTime time) {
        // Find which doctor corresponds to this column
        String doctorId = null;
        String doctorName = "";

        for (Map.Entry<String, Integer> entry : doctorColumnMap.entrySet()) {
            if (entry.getValue() == doctorColumn) {
                doctorId = entry.getKey();

                // Find doctor name
                for (User doctor : doctors) {
                    if (doctor.getId().equals(doctorId)) {
                        doctorName = doctor.getFullName();
                        break;
                    }
                }
                break;
            }
        }

        if (doctorId == null) {
            // No doctor for this column
            return;
        }

        // Create appointment at this time for this doctor
        Appointment newAppointment = new Appointment();
        newAppointment.setMedicinID(doctorId);
        newAppointment.setDoctorName(doctorName);

        // Set the appointment time
        LocalDateTime appointmentTime = LocalDateTime.of(currentDate, time);
        newAppointment.setAppointmentDateTime(appointmentTime);

        // Open appointment form
        openAppointmentForm(newAppointment);
    }

    private void updateDateLabel() {
        currentDateLabel.setText(currentDate.format(dateFormatter));
    }

    public void updateCalendarView() {
        // Clear existing appointments from grid
        clearAppointmentsFromGrid();

        // Get filter values
        User selectedDoctor = doctorFilter.getValue();
        String selectedStatus = statusFilter.getValue();

        // Get appointments for the current date
        List<Appointment> appointments = appointmentManager.getAppointmentsByDate(currentDate);

        // Filter appointments based on selected filters
        List<Appointment> filteredAppointments = new ArrayList<>();
        for (Appointment appointment : appointments) {
            boolean includeAppointment = true;

            // Filter by doctor
            if (selectedDoctor != null && !"Tous".equals(selectedDoctor.getFirstName())) {
                if (appointment.getMedicinID() == null || !appointment.getMedicinID().equals(selectedDoctor.getId())) {
                    includeAppointment = false;
                }
            }

            // Filter by status
            if (!"Tous les statuts".equals(selectedStatus)) {
                // Map UI status (in French) back to internal status values (in English)
                String internalStatus = mapFrenchStatusToInternal(selectedStatus);
                if (!internalStatus.equals(appointment.getStatus())) {
                    includeAppointment = false;
                }
            }

            if (includeAppointment) {
                filteredAppointments.add(appointment);
            }
        }

        // Debug log
        System.out.println("Found " + appointments.size() + " appointments for date " + currentDate);
        System.out.println("After filtering: " + filteredAppointments.size() + " appointments");

        // Display appointments in the grid
        displayAppointmentsInGrid(filteredAppointments);

        // Update statistics
        updateStatistics(appointments);

        // Update upcoming appointments
        updateUpcomingAppointments(appointments);
    }

    // Helper method to map French status labels to internal status values
    private String mapFrenchStatusToInternal(String frenchStatus) {
        switch (frenchStatus) {
            case "Programmé": return "Scheduled";
            case "Enregistré": return "CheckedIn";
            case "En cours": return "InProgress";
            case "Terminé": return "Completed";
            case "Manqué": return "Missed";
            case "Annulé": return "Cancelled";
            default: return frenchStatus; // Fallback
        }
    }

    // Helper method to map internal status values to French labels
    private String mapInternalStatusToFrench(String internalStatus) {
        switch (internalStatus) {
            case "Scheduled": return "Programmé";
            case "CheckedIn": return "Enregistré";
            case "InProgress": return "En cours";
            case "Completed": return "Terminé";
            case "Missed": return "Manqué";
            case "Patient_Cancelled":
            case "Clinic_Cancelled":
            case "Cancelled": return "Annulé";
            case "Rescheduled": return "Reprogrammé";
            default: return internalStatus; // Fallback
        }
    }

    private void clearAppointmentsFromGrid() {
        // Remove all appointment blocks from the grid
        // We start from row 1 to keep the header
        for (int row = 1; row < calendarGrid.getRowConstraints().size(); row++) {
            for (int col = 1; col <= 4; col++) {
                // Find all children at this position that are appointment blocks
                List<Node> toRemove = new ArrayList<>();
                for (Node node : calendarGrid.getChildren()) {
                    if (node instanceof VBox &&
                            GridPane.getRowIndex(node) == row &&
                            GridPane.getColumnIndex(node) == col &&
                            node.getStyleClass().contains("appointment-block")) {
                        toRemove.add(node);
                    }
                }

                // Remove the appointment blocks
                calendarGrid.getChildren().removeAll(toRemove);
            }
        }
    }

    private void displayAppointmentsInGrid(List<Appointment> appointments) {
        for (Appointment appointment : appointments) {
            // Get the doctor column
            Integer column = doctorColumnMap.get(appointment.getMedicinID());

            if (column == null) {
                System.out.println("Doctor column not found for doctor ID: " + appointment.getMedicinID());

                // If doctor is not in current view but there's at least one column, use the first column
                if (!doctorColumnMap.isEmpty()) {
                    column = doctorColumnMap.values().iterator().next();
                } else {
                    // Default to column 1 if no columns are mapped
                    column = 1;
                }
            }

            // Get the time row
            LocalTime appointmentTime = appointment.getAppointmentDateTime().toLocalTime();
            int row = getRowForTime(appointmentTime);

            if (row == -1) {
                System.out.println("Time not in calendar range: " + appointmentTime);
                continue;
            }

            System.out.println("Adding appointment to grid: " + appointment.getRendezVousID() +
                    " at column " + column + ", row " + row +
                    " for time " + appointmentTime);

            // Create appointment block
            VBox appointmentBlock = createAppointmentBlock(appointment);

            // Add it to the grid
            calendarGrid.add(appointmentBlock, column, row);
        }
    }

    private int getRowForTime(LocalTime time) {
        // Calculate row index based on time (8:00 = row 1, 8:15 = row 2, 8:30 = row 3, etc.)
        int hour = time.getHour();
        int minute = time.getMinute();

        if (hour < 8 || hour > 17 || (hour == 17 && minute > 45)) {
            // Outside of calendar range
            return -1;
        }

        // Calculate row: (hour - 8) * 4 + (minute / 15) + 1
        // +1 because row 0 is header
        return (hour - 8) * 4 + (minute / 15) + 1;
    }

    private VBox createAppointmentBlock(Appointment appointment) {
        VBox appointmentBlock = new VBox();
        appointmentBlock.getStyleClass().addAll("appointment-block", "appointment-" + appointment.getStatus().toLowerCase());

        // CRITICAL: Remove height restrictions to allow content to determine size
        // Do NOT set setPrefHeight or setMaxHeight here
        appointmentBlock.setSpacing(2);

        // Store appointment ID in the block for later reference
        appointmentBlock.setUserData(appointment);

        // If urgent, add additional style
        if ("Urgent".equals(appointment.getPriority())) {
            appointmentBlock.getStyleClass().add("appointment-urgent");
        }

        // REMOVED: Time label - not needed as time is shown in row header

        // Patient name - with direct styling to ensure visibility
        Label patientLabel = new Label(appointment.getPatientName());
        patientLabel.getStyleClass().add("appointment-patient");
        patientLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;");
        patientLabel.setWrapText(true);
        patientLabel.setMaxWidth(Double.MAX_VALUE);

        // Reason for visit - do not truncate anymore
        String reasonText = appointment.getReasonForVisit();
        // REMOVED: Code that truncated reason to 15 characters

        Label reasonLabel = new Label(reasonText);
        reasonLabel.getStyleClass().add("appointment-reason");
        reasonLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white;");
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(Double.MAX_VALUE);

        // Add components to block (without time label)
        appointmentBlock.getChildren().addAll(patientLabel, reasonLabel);

        // Add click handler
        appointmentBlock.setOnMouseClicked(e -> handleAppointmentClick(appointment));

        return appointmentBlock;
    }

    private void handleAppointmentClick(Appointment appointment) {
        // Show appointment actions dialog
        showAppointmentActionsDialog(appointment);
    }

    private void showAppointmentActionsDialog(Appointment appointment) {
        // Create a popup with quick actions for this appointment
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Actions pour le Rendez-vous");
        dialog.setHeaderText("Patient: " + appointment.getPatientName() +
                "\nHeure: " + appointment.getFormattedTime() +
                "\nStatut: " + mapInternalStatusToFrench(appointment.getStatus()));

        // Add buttons based on current status
        ButtonType viewEditType = new ButtonType("Voir/Modifier", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().add(viewEditType);

        if ("Scheduled".equals(appointment.getStatus())) {
            ButtonType checkInType = new ButtonType("Enregistrer l'arrivée", ButtonBar.ButtonData.LEFT);
            ButtonType missedType = new ButtonType("Absence", ButtonBar.ButtonData.LEFT);
            ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.LEFT);

            dialog.getDialogPane().getButtonTypes().addAll(checkInType, missedType, cancelType);
        } else if ("CheckedIn".equals(appointment.getStatus())) {
            ButtonType startType = new ButtonType("Démarrer la consultation", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(startType);
        } else if ("InProgress".equals(appointment.getStatus())) {
            ButtonType completeType = new ButtonType("Terminer", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(completeType);
        } else if ("Missed".equals(appointment.getStatus())) {
            ButtonType rescheduleType = new ButtonType("Reprogrammer", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(rescheduleType);
        }

        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        // Handle button clicks
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            ButtonType buttonType = result.get();

            if (buttonType == viewEditType) {
                openAppointmentForm(appointment);
            } else if (buttonType.getText().equals("Enregistrer l'arrivée")) {
                handleCheckIn(appointment);
            } else if (buttonType.getText().equals("Démarrer la consultation")) {
                handleStartAppointment(appointment);
            } else if (buttonType.getText().equals("Terminer")) {
                handleCompleteAppointment(appointment);
            } else if (buttonType.getText().equals("Absence")) {
                handleMissedAppointment(appointment);
            } else if (buttonType.getText().equals("Annuler")) {
                handleCancelAppointment(appointment);
            } else if (buttonType.getText().equals("Reprogrammer")) {
                handleRescheduleAppointment(appointment);
            }
        }
    }

    private void handleCheckIn(Appointment appointment) {
        boolean success = appointmentManager.checkInAppointment(appointment.getRendezVousID());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Patient enregistré avec succès.");
            updateCalendarView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'enregistrement du patient.");
        }
    }

    private void handleStartAppointment(Appointment appointment) {
        boolean success = appointmentManager.startAppointment(appointment.getRendezVousID());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Consultation démarrée.");
            updateCalendarView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du démarrage de la consultation.");
        }
    }

    private void handleCompleteAppointment(Appointment appointment) {
        // Create a dialog to get notes
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Terminer la Consultation");
        dialog.setHeaderText("Ajouter des notes de fin (facultatif)");
        dialog.setContentText("Notes :");

        Optional<String> result = dialog.showAndWait();
        String notes = result.orElse("");

        boolean success = appointmentManager.completeAppointment(appointment.getRendezVousID(), notes);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Consultation terminée.");
            updateCalendarView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la finalisation de la consultation.");
        }
    }

    private void handleMissedAppointment(Appointment appointment) {
        // Create a dialog to get reason
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Absence au Rendez-vous");
        dialog.setHeaderText("Indiquer la raison de l'absence");
        dialog.setContentText("Raison :");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String reason = result.get();
            boolean success = appointmentManager.markAsMissed(appointment.getRendezVousID(), reason);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous marqué comme manqué.");

                // Try to shift checked-in appointments after updating status
                shiftCheckedInAppointments(appointment);

                updateCalendarView();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour du rendez-vous.");
            }
        }
    }

    private void handleCancelAppointment(Appointment appointment) {
        // Create a dialog to select cancellation type and reason
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Annuler le Rendez-vous");
        dialog.setHeaderText("Annuler le rendez-vous pour " + appointment.getPatientName());

        // Set the button types
        ButtonType cancelButtonType = new ButtonType("Annuler le Rendez-vous", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, ButtonType.CANCEL);

        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ToggleGroup cancellationTypeGroup = new ToggleGroup();
        RadioButton patientCancelled = new RadioButton("Annulé par le Patient");
        patientCancelled.setToggleGroup(cancellationTypeGroup);
        patientCancelled.setSelected(true);

        RadioButton clinicCancelled = new RadioButton("Annulé par la Clinique");
        clinicCancelled.setToggleGroup(cancellationTypeGroup);

        TextField reasonField = new TextField();
        reasonField.setPromptText("Raison de l'annulation");

        grid.add(new Label("Type d'annulation :"), 0, 0);
        grid.add(patientCancelled, 1, 0);
        grid.add(clinicCancelled, 1, 1);
        grid.add(new Label("Raison :"), 0, 2);
        grid.add(reasonField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == cancelButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("type", patientCancelled.isSelected() ? "patient" : "clinic");
                result.put("reason", reasonField.getText());
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            boolean isByPatient = "patient".equals(data.get("type"));
            String reason = data.get("reason");

            boolean success = appointmentManager.cancelAppointment(
                    appointment.getRendezVousID(), reason, isByPatient);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous annulé avec succès.");

                // Try to shift checked-in appointments after cancellation
                shiftCheckedInAppointments(appointment);

                updateCalendarView();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'annulation du rendez-vous.");
            }
        });
    }

    /**
     * Shift checked-in appointments to fill the slots of cancelled or missed appointments
     * Only shifts consecutive checked-in appointments until a non-checked-in appointment is encountered
     * @param cancelledAppointment The appointment that was cancelled or missed
     */
    private void shiftCheckedInAppointments(Appointment cancelledAppointment) {
        // Get all appointments for the current day
        List<Appointment> appointments = appointmentManager.getAppointmentsByDate(currentDate);

        // Get the time of the cancelled appointment
        LocalDateTime cancelledTime = cancelledAppointment.getAppointmentDateTime();
        String doctorId = cancelledAppointment.getMedicinID();

        // Sort all doctor's appointments by time
        List<Appointment> doctorAppointments = appointments.stream()
                .filter(appt -> appt.getMedicinID().equals(doctorId))
                .sorted(Comparator.comparing(Appointment::getAppointmentDateTime))
                .collect(java.util.stream.Collectors.toList());

        // Find the consecutive checked-in appointments after the cancelled appointment
        List<Appointment> consecutiveCheckedIn = new ArrayList<>();
        boolean foundCancelled = false;

        for (Appointment appt : doctorAppointments) {
            // First, find our cancelled appointment in the sequence
            if (!foundCancelled) {
                if (appt.getRendezVousID() == cancelledAppointment.getRendezVousID()) {
                    foundCancelled = true;
                }
                continue;
            }

            // After finding the cancelled appointment, collect consecutive checked-in appointments
            if ("CheckedIn".equals(appt.getStatus())) {
                consecutiveCheckedIn.add(appt);
            } else {
                // Stop as soon as we find a non-checked-in appointment
                break;
            }
        }

        // If we have any consecutive checked-in appointments that can be moved
        if (!consecutiveCheckedIn.isEmpty()) {
            // Ask user if they want to shift all applicable appointments
            Alert confirmAllShifts = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAllShifts.setTitle("Avancer les Rendez-vous");
            confirmAllShifts.setHeaderText("Il y a " + consecutiveCheckedIn.size() +
                    " patient(s) consécutifs déjà enregistré(s) qui peuvent être avancés");
            confirmAllShifts.setContentText("Souhaitez-vous avancer ces rendez-vous pour combler le créneau libéré ?");

            Optional<ButtonType> allResult = confirmAllShifts.showAndWait();
            if (allResult.isPresent() && allResult.get() == ButtonType.OK) {
                // Process each appointment in order
                LocalDateTime availableTime = cancelledTime;
                List<Appointment> successfullyShifted = new ArrayList<>();

                for (Appointment appointmentToShift : consecutiveCheckedIn) {
                    // Store the original time before changing it
                    LocalDateTime originalTime = appointmentToShift.getAppointmentDateTime();
                    String patientName = appointmentToShift.getPatientName();

                    // Change the appointment time
                    boolean success = appointmentManager.changeAppointmentTime(
                            appointmentToShift.getRendezVousID(),
                            availableTime,
                            "Avancé pour remplacer un rendez-vous annulé/manqué");

                    if (success) {
                        // Add to list of successfully shifted appointments
                        successfullyShifted.add(appointmentToShift);

                        // Update the available time for the next appointment to shift
                        availableTime = originalTime;
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur",
                                "Impossible d'avancer le rendez-vous de " + patientName);
                        break; // Stop processing if an error occurs
                    }
                }

                // Show a summary of all shifted appointments
                if (!successfullyShifted.isEmpty()) {
                    StringBuilder message = new StringBuilder("Les rendez-vous suivants ont été avancés :\n");
                    for (Appointment shifted : successfullyShifted) {
                        message.append("- ")
                                .append(shifted.getPatientName())
                                .append(" à ")
                                .append(shifted.getAppointmentDateTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                                .append("\n");
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Succès", message.toString());
                }
            }
        }
    }

    private void handleRescheduleAppointment(Appointment appointment) {
        // Create dialog for rescheduling
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Reprogrammer le Rendez-vous");
        dialog.setHeaderText("Reprogrammer le rendez-vous pour " + appointment.getPatientName());

        // Set the button types
        ButtonType rescheduleButtonType = new ButtonType("Reprogrammer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rescheduleButtonType, ButtonType.CANCEL);

        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> timeComboBox = new ComboBox<>();

        // Populate time options (8:00 AM to 5:45 PM in 15-min increments)
        ObservableList<String> timeOptions = FXCollections.observableArrayList();
        LocalTime time = LocalTime.of(8, 0);
        while (!time.isAfter(LocalTime.of(17, 45))) {
            timeOptions.add(time.format(timeFormatter));
            time = time.plusMinutes(15);
        }
        timeComboBox.setItems(timeOptions);
        timeComboBox.getSelectionModel().select(0);

        TextField reasonField = new TextField();
        reasonField.setPromptText("Raison du changement");

        grid.add(new Label("Nouvelle date :"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Nouvelle heure :"), 0, 1);
        grid.add(timeComboBox, 1, 1);
        grid.add(new Label("Raison :"), 0, 2);
        grid.add(reasonField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == rescheduleButtonType) {
                Map<String, Object> result = new HashMap<>();
                result.put("date", datePicker.getValue());
                result.put("time", timeComboBox.getValue());
                result.put("reason", reasonField.getText());
                return result;
            }
            return null;
        });

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            LocalDate newDate = (LocalDate) data.get("date");
            String timeString = (String) data.get("time");
            String reason = (String) data.get("reason");

            LocalTime newTime = LocalTime.parse(timeString, timeFormatter);
            LocalDateTime newDateTime = LocalDateTime.of(newDate, newTime);

            // Store the old appointment data before rescheduling
            LocalDateTime oldDateTime = appointment.getAppointmentDateTime();
            String doctorId = appointment.getMedicinID();

            int newAppointmentId = appointmentManager.rescheduleAppointment(
                    appointment.getRendezVousID(), newDateTime, reason);

            if (newAppointmentId > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Rendez-vous reprogrammé avec succès.");

                // Only try to shift appointments if the rescheduled appointment was for today
                if (oldDateTime.toLocalDate().equals(currentDate)) {
                    // Create a temporary appointment object with the old data for shifting
                    Appointment oldAppointment = new Appointment();
                    oldAppointment.setAppointmentDateTime(oldDateTime);
                    oldAppointment.setMedicinID(doctorId);

                    // Try to shift checked-in appointments
                    shiftCheckedInAppointments(oldAppointment);
                }

                updateCalendarView();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la reprogrammation du rendez-vous.");
            }
        });
    }

    private void updateStatistics(List<Appointment> appointments) {
        int total = appointments.size();
        int completed = 0;
        int inProgress = 0;
        int checkedIn = 0;
        int scheduled = 0;
        int missed = 0;
        int cancelled = 0; // Added new counter for cancelled appointments

        for (Appointment appointment : appointments) {
            switch (appointment.getStatus()) {
                case "Completed":
                    completed++;
                    break;
                case "InProgress":
                    inProgress++;
                    break;
                case "CheckedIn":
                    checkedIn++;
                    break;
                case "Scheduled":
                    scheduled++;
                    break;
                case "Missed":
                    missed++;
                    break;
                // Handle all cancellation types in the same counter
                case "Cancelled":
                case "Patient_Cancelled":
                case "Clinic_Cancelled":
                    cancelled++;
                    break;
            }
        }

        totalAppointmentsLabel.setText(Integer.toString(total));
        completedAppointmentsLabel.setText(Integer.toString(completed));
        inProgressAppointmentsLabel.setText(Integer.toString(inProgress));
        checkedInAppointmentsLabel.setText(Integer.toString(checkedIn));
        scheduledAppointmentsLabel.setText(Integer.toString(scheduled));
        missedAppointmentsLabel.setText(Integer.toString(missed));
        cancelledAppointmentsLabel.setText(Integer.toString(cancelled)); // Set the cancelled count
    }

    private void updateUpcomingAppointments(List<Appointment> appointments) {
        // Clear existing items
        upcomingAppointmentsBox.getChildren().clear();

        // Get current time
        LocalDateTime now = LocalDateTime.now();

        // Filter and sort upcoming appointments
        List<Appointment> upcoming = new ArrayList<>();
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentDateTime().isAfter(now) &&
                    ("Scheduled".equals(appointment.getStatus()) ||
                            "CheckedIn".equals(appointment.getStatus()))) {
                upcoming.add(appointment);
            }
        }

        // Sort by time
        upcoming.sort(Comparator.comparing(Appointment::getAppointmentDateTime));

        // Display up to 5 upcoming appointments
        int count = 0;
        for (Appointment appointment : upcoming) {
            if (count >= 5) break;

            HBox appointmentItem = createUpcomingAppointmentItem(appointment);
            upcomingAppointmentsBox.getChildren().add(appointmentItem);
            count++;
        }

        // Add "No upcoming appointments" message if none
        if (upcoming.isEmpty()) {
            Label noAppointmentsLabel = new Label("Aucun rendez-vous à venir");
            noAppointmentsLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
            upcomingAppointmentsBox.getChildren().add(noAppointmentsLabel);
        }
    }

    private HBox createUpcomingAppointmentItem(Appointment appointment) {
        HBox item = new HBox(5);
        item.getStyleClass().add("upcoming-appointment");

        // Time
        Label timeLabel = new Label(appointment.getFormattedTime());
        timeLabel.getStyleClass().add("upcoming-time");

        // Patient name and doctor
        VBox detailsBox = new VBox(2);
        Label patientLabel = new Label(appointment.getPatientName());
        patientLabel.getStyleClass().add("upcoming-patient");

        Label doctorLabel = new Label("Dr. " + appointment.getDoctorName());
        doctorLabel.getStyleClass().add("upcoming-doctor");

        detailsBox.getChildren().addAll(patientLabel, doctorLabel);

        // Action button
        Button actionButton = new Button();
        actionButton.getStyleClass().add("upcoming-action");
        FontIcon icon = new FontIcon("fas-eye");
        icon.setIconSize(14);
        actionButton.setGraphic(icon);

        actionButton.setOnAction(e -> handleAppointmentClick(appointment));

        // Add to item
        item.getChildren().addAll(timeLabel, detailsBox);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);
        item.getChildren().add(actionButton);

        return item;
    }

    private void setupAutoRefresh() {
        // Cancel existing timer if any
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        // Create new timer to refresh every 5 minutes
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateCalendarView());
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000);
    }

    @FXML
    private void previousDay(ActionEvent event) {
        currentDate = currentDate.minusDays(1);
        updateDateLabel();
        updateCalendarView();
    }

    @FXML
    private void nextDay(ActionEvent event) {
        currentDate = currentDate.plusDays(1);
        updateDateLabel();
        updateCalendarView();
    }

    @FXML
    private void goToToday(ActionEvent event) {
        currentDate = LocalDate.now();
        updateDateLabel();
        updateCalendarView();
    }

    @FXML
    private void newAppointment(ActionEvent event) {
        openAppointmentForm(null);
    }

    private void openAppointmentForm(Appointment appointment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/myjavafxapp/AppointmentForm.fxml"));

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(appointment == null ? "Nouveau Rendez-vous" : "Modifier le Rendez-vous");
            dialog.setScene(new Scene(loader.load()));

            // Set this controller as user data so the form can call back to refresh
            dialog.setUserData(this);

            AppointmentFormController controller = loader.getController();
            controller.setAppointment(appointment);

            dialog.showAndWait();

            // Always refresh the view after dialog closes
            updateCalendarView();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de rendez-vous : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /* Methods for switching scenes*/

    @FXML
    public void onHome(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/CalendarView.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers le Tableau de bord : " + e.getMessage());
        }
    }

    @FXML
    public void onCalendar(ActionEvent event) {
        // We're already on the calendar view, so just refresh
        updateCalendarView();
    }

    @FXML
    public void onCreditCard(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/GestionPaiment.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la Gestion des Paiements : " + e.getMessage());
        }
    }

    @FXML
    public void onFolder(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/PatientRecordsView.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers les Dossiers Patients : " + e.getMessage());
        }
    }

    @FXML
    public void onGlobalStats(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/StatisticsDashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers les Statistiques : " + e.getMessage());
        }
    }

    @FXML
    public void onRepport(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/rapportQuotidien.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers les Rapports Quotidiens : " + e.getMessage());
        }
    }

    @FXML
    public void onBackUp(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/sauvegarde.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la Sauvegarde : " + e.getMessage());
        }
    }

    @FXML
    public void onUsers(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/Users.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la Gestion des Utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    public void onLogOut(ActionEvent event) {
        try {
            // Clear user session
            UserSession.getInstance().setUsername(null);
            UserSession.getInstance().setRole(null);

            SwitchScene.switchScene(event, "/com/example/myjavafxapp/loginForm.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la déconnexion : " + e.getMessage());
        }
    }
}