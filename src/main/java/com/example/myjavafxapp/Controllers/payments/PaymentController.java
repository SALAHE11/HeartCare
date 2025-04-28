package com.example.myjavafxapp.Controllers.payments;

import com.example.myjavafxapp.Models.appointment.Appointment;
import com.example.myjavafxapp.Models.payment.Payment;
import com.example.myjavafxapp.Models.payment.PaymentManager;
import com.example.myjavafxapp.Models.user.UserSession;
import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.util.SwitchScene;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PaymentController implements Initializable {

    @FXML private TextField patientCINField;
    @FXML private Button searchButton;
    @FXML private Button returnToCalendarButton;
    @FXML private DatePicker paymentDatePicker;
    @FXML private Button filterPaymentDateButton;

    // Appointment table
    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, Integer> appointmentIdColumn;
    @FXML private TableColumn<Appointment, String> appointmentDateColumn;
    @FXML private TableColumn<Appointment, String> appointmentTimeColumn;
    @FXML private TableColumn<Appointment, String> patientNameColumn;
    @FXML private TableColumn<Appointment, String> patientCINColumn;
    @FXML private TableColumn<Appointment, String> doctorNameColumn;
    @FXML private TableColumn<Appointment, String> reasonColumn;
    @FXML private TableColumn<Appointment, Boolean> isPaidColumn;
    @FXML private TableColumn<Appointment, Void> actionsColumn;

    // Payment table
    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, Integer> paymentIdColumn;
    @FXML private TableColumn<Payment, String> paymentDateColumn;
    @FXML private TableColumn<Payment, String> paymentPatientNameColumn;
    @FXML private TableColumn<Payment, String> paymentPatientCINColumn;
    @FXML private TableColumn<Payment, Integer> appointmentIdPaymentColumn;
    @FXML private TableColumn<Payment, Double> amountColumn;
    @FXML private TableColumn<Payment, String> paymentMethodColumn;
    @FXML private TableColumn<Payment, Void> paymentActionsColumn;

    // Date formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Models and data
    private PaymentManager paymentManager = PaymentManager.getInstance();
    private ObservableList<Appointment> appointmentsData = FXCollections.observableArrayList();
    private ObservableList<Payment> paymentsData = FXCollections.observableArrayList();

    // French display values for the UI
    private static final ObservableList<String> PAYMENT_METHOD_DISPLAY_OPTIONS = FXCollections.observableArrayList(
            "Espèces",            // Cash
            "Carte de Crédit",    // Credit Card
            "Assurance"           // Insurance
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupAppointmentTable();
        setupPaymentTable();

        // Set default date for payment date picker to today
        if (paymentDatePicker != null) {
            paymentDatePicker.setValue(LocalDate.now());
        }

        // Load all payments for the payment history table (filtered for today by default)
        loadPaymentHistory();

        // Load only today's completed unpaid appointments
        loadTodayUnpaidCompletedAppointments();
    }

    /**
     * Load today's unpaid completed appointments
     */
    private void loadTodayUnpaidCompletedAppointments() {
        List<Appointment> todayUnpaidAppointments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Get today's date in SQL format
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Query to get today's completed appointments
            String query = "SELECT r.*, " +
                    "p.FNAME AS patientFirstName, p.LNAME AS patientLastName, " +
                    "u.FNAME AS doctorFirstName, u.LNAME AS doctorLastName " +
                    "FROM rendezvous r " +
                    "JOIN patient p ON r.PatientID = p.ID " +
                    "JOIN users u ON r.MedecinID = u.ID AND u.ROLE = 'medecin' " +
                    "WHERE r.Status = 'Completed' " +
                    "AND DATE(r.AppointmentDateTime) = ? " +
                    "AND NOT EXISTS (SELECT 1 FROM paiment pay WHERE pay.RendezVousID = r.RendezVousID) " +
                    "ORDER BY r.AppointmentDateTime";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, todayStr);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Appointment appointment = new Appointment();

                appointment.setRendezVousID(rs.getInt("RendezVousID"));
                appointment.setPatientID(rs.getString("PatientID"));

                // Combine first and last name for patient
                String patientFirstName = rs.getString("patientFirstName");
                String patientLastName = rs.getString("patientLastName");
                appointment.setPatientName(patientFirstName + " " + patientLastName);

                appointment.setMedicinID(rs.getString("MedecinID"));

                // Combine first and last name for doctor
                String doctorFirstName = rs.getString("doctorFirstName");
                String doctorLastName = rs.getString("doctorLastName");
                appointment.setDoctorName(doctorFirstName + " " + doctorLastName);

                // Parse datetime
                Timestamp appointmentTimestamp = rs.getTimestamp("AppointmentDateTime");
                if (appointmentTimestamp != null) {
                    appointment.setAppointmentDateTime(appointmentTimestamp.toLocalDateTime());
                }

                appointment.setReasonForVisit(rs.getString("ReasonForVisit"));
                appointment.setStatus(rs.getString("Status"));

                todayUnpaidAppointments.add(appointment);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement des rendez-vous: " + e.getMessage());
        }

        appointmentsData.setAll(todayUnpaidAppointments);

        if (todayUnpaidAppointments.isEmpty()) {
            // Only show alert if there are no appointments at startup
            // Don't show alert when payment was just processed - it's expected there are no more unpaid appointments
            if (!paymentJustProcessed) {
                showAlert(Alert.AlertType.INFORMATION, "Information",
                        "Aucun rendez-vous terminé non payé pour aujourd'hui.");
            }
        }

        // Reset flag
        paymentJustProcessed = false;
    }

    // Track if payment was just processed to avoid unnecessary alerts
    private boolean paymentJustProcessed = false;

    /**
     * Set up the appointment table columns
     */
    private void setupAppointmentTable() {
        appointmentIdColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getRendezVousID()).asObject());

        appointmentDateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAppointmentDateTime().format(dateFormatter)));

        appointmentTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAppointmentDateTime().format(timeFormatter)));

        patientNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatientName()));

        patientCINColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatientID()));

        doctorNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDoctorName()));

        reasonColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getReasonForVisit()));

        isPaidColumn.setCellValueFactory(cellData -> {
            // Force a direct database check for payment status on each cell update
            boolean isPaid = checkAppointmentIsPaid(cellData.getValue().getRendezVousID());
            return new SimpleBooleanProperty(isPaid);
        });

        // Set up the actions column with buttons
        actionsColumn.setCellFactory(col -> new TableCell<Appointment, Void>() {
            private final Button payButton = new Button();

            {
                // Configure pay button
                FontIcon payIcon = new FontIcon("fas-money-bill");
                payIcon.setIconSize(14);
                payButton.setGraphic(payIcon);
                payButton.getStyleClass().add("action-button");
                payButton.setTooltip(new Tooltip("Enregistrer paiement"));

                payButton.setOnAction(e -> {
                    Appointment appointment = getTableView().getItems().get(getIndex());
                    showPaymentDialog(appointment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Appointment appointment = getTableView().getItems().get(getIndex());

                    // Force a new database check for payment status
                    boolean isPaid = checkAppointmentIsPaid(appointment.getRendezVousID());

                    if (isPaid) {
                        // If already paid, show "Paid" label
                        Label paidLabel = new Label("Payé");
                        paidLabel.getStyleClass().add("success-message");
                        setGraphic(paidLabel);
                    } else {
                        // If not paid, show pay button
                        setGraphic(payButton);
                    }
                }
            }
        });

        appointmentTable.setItems(appointmentsData);
    }

    /**
     * Force a new database check for payment status to avoid cached results
     */
    private boolean checkAppointmentIsPaid(int rendezVousID) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Use a fresh connection and transaction to ensure current data
            String query = "SELECT COUNT(*) FROM paiment WHERE RendezVousID = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, rendezVousID);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Remove a paid appointment from the table
     */
    private void removeAppointmentById(int rendezVousID) {
        appointmentsData.removeIf(appointment -> appointment.getRendezVousID() == rendezVousID);
    }

    /**
     * Set up the payment table columns
     */
    private void setupPaymentTable() {
        paymentIdColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getPaymentID()).asObject());

        paymentDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getPaymentDate() != null) {
                LocalDateTime dateTime = cellData.getValue().getPaymentDate().toLocalDateTime();
                return new SimpleStringProperty(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("");
        });

        paymentPatientNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatientName()));

        paymentPatientCINColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatientID()));

        appointmentIdPaymentColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getRendezVousID()).asObject());

        amountColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());

        paymentMethodColumn.setCellValueFactory(cellData -> {
            // Display the payment method in French
            String dbMethod = cellData.getValue().getPaymentMethod();
            String displayMethod = mapDBValueToDisplay(dbMethod);
            return new SimpleStringProperty(displayMethod);
        });

        // Set up the actions column with edit and print buttons

        paymentActionsColumn.setCellFactory(col -> new TableCell<Payment, Void>() {
            private final Button editButton = new Button();
            private final Button printButton = new Button();
            // Remove delete button declaration and initialization

            {
                // Configure edit button
                FontIcon editIcon = new FontIcon("fas-edit");
                editIcon.setIconSize(14);
                editButton.setGraphic(editIcon);
                editButton.getStyleClass().add("icon-button");
                editButton.setTooltip(new Tooltip("Modifier paiement"));

                editButton.setOnAction(e -> {
                    Payment payment = getTableView().getItems().get(getIndex());
                    showEditPaymentDialog(payment);
                });

                // Configure print button
                FontIcon printIcon = new FontIcon("fas-print");
                printIcon.setIconSize(14);
                printButton.setGraphic(printIcon);
                printButton.getStyleClass().add("icon-button");
                printButton.setTooltip(new Tooltip("Imprimer facture"));

                printButton.setOnAction(e -> {
                    Payment payment = getTableView().getItems().get(getIndex());
                    printInvoice(payment);
                });

                // Remove delete button configuration code
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Payment payment = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);

                    // Get payment date
                    boolean isPaymentFromToday = false;
                    if (payment.getPaymentDate() != null) {
                        LocalDate paymentDate = payment.getPaymentDate().toLocalDateTime().toLocalDate();
                        isPaymentFromToday = paymentDate.equals(LocalDate.now());
                    }

                    // Only allow editing payments from today
                    if (isPaymentFromToday) {
                        buttons.getChildren().add(editButton);
                        // Remove adding delete button
                    }

                    // Always allow printing
                    buttons.getChildren().add(printButton);

                    setGraphic(buttons);
                }
            }
        });

        paymentTable.setItems(paymentsData);
    }

    /**
     * Print invoice as PDF for a payment
     */
    private void printInvoice(Payment payment) {
        try {
            // Get appointment details
            Appointment appointment = paymentManager.getAppointmentById(payment.getRendezVousID());

            if (appointment == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de trouver les détails du rendez-vous.");
                return;
            }

            // Create invoice content
            VBox invoiceContent = createInvoiceContent(payment, appointment);

            // Create and configure PrinterJob
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null) {
                // Set up page layout
                PageLayout pageLayout = job.getPrinter().createPageLayout(
                        Paper.A4, PageOrientation.PORTRAIT, 50, 50, 50, 50);
                job.getJobSettings().setPageLayout(pageLayout);

                // Show printer dialog
                boolean proceed = job.showPrintDialog(null);

                if (proceed) {
                    // Print the invoice
                    boolean printed = job.printPage(invoiceContent);

                    if (printed) {
                        job.endJob();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Facture imprimée avec succès.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'impression de la facture.");
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de créer un travail d'impression.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'impression: " + e.getMessage());
        }
    }

    /**
     * Create invoice content
     */
    private VBox createInvoiceContent(Payment payment, Appointment appointment) {
        VBox invoice = new VBox(20);
        invoice.setStyle("-fx-padding: 20; -fx-background-color: white;");

        // Title "FACTURE"
        Text headerText = new Text("FACTURE");
        headerText.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        // Clinic info
        Text clinicInfo = new Text("HeartCare Medical Center\n" +
                "123 Rue de la Santé\n" +
                "Casablanca, Maroc\n" +
                "Tel: +212 522 123456\n" +
                "Email: contact@heartcare.ma");
        clinicInfo.setFont(Font.font("Arial", 12));

        // Invoice details
        GridPane invoiceDetails = new GridPane();
        invoiceDetails.setHgap(10);
        invoiceDetails.setVgap(5);

        invoiceDetails.add(createBoldText("N° Facture:"), 0, 0);
        invoiceDetails.add(new Text("F-" + payment.getPaymentID()), 1, 0);

        invoiceDetails.add(createBoldText("Date:"), 0, 1);
        invoiceDetails.add(new Text(payment.getPaymentDate().toLocalDateTime().format(dateTimeFormatter)), 1, 1);

        invoiceDetails.add(createBoldText("N° RDV:"), 0, 2);
        invoiceDetails.add(new Text(String.valueOf(payment.getRendezVousID())), 1, 2);

        // Patient details
        Text patientHeaderText = createSectionHeader("PATIENT");

        GridPane patientDetails = new GridPane();
        patientDetails.setHgap(10);
        patientDetails.setVgap(5);

        patientDetails.add(createBoldText("Nom:"), 0, 0);
        patientDetails.add(new Text(payment.getPatientName()), 1, 0);

        patientDetails.add(createBoldText("CIN:"), 0, 1);
        patientDetails.add(new Text(payment.getPatientID()), 1, 1);

        // Appointment details
        Text appointmentHeaderText = createSectionHeader("DÉTAILS DU RENDEZ-VOUS");

        GridPane appointmentDetails = new GridPane();
        appointmentDetails.setHgap(10);
        appointmentDetails.setVgap(5);

        appointmentDetails.add(createBoldText("Date:"), 0, 0);
        appointmentDetails.add(new Text(appointment.getFormattedDateTime()), 1, 0);

        appointmentDetails.add(createBoldText("Médecin:"), 0, 1);
        appointmentDetails.add(new Text(appointment.getDoctorName()), 1, 1);

        appointmentDetails.add(createBoldText("Motif:"), 0, 2);
        appointmentDetails.add(new Text(appointment.getReasonForVisit()), 1, 2);

        // Payment details
        Text paymentHeaderText = createSectionHeader("DÉTAILS DU PAIEMENT");

        GridPane paymentDetails = new GridPane();
        paymentDetails.setHgap(10);
        paymentDetails.setVgap(5);

        paymentDetails.add(createBoldText("Montant:"), 0, 0);
        paymentDetails.add(new Text(String.format("%.2f DH", payment.getAmount())), 1, 0);

        paymentDetails.add(createBoldText("Méthode:"), 0, 1);
        // Display the payment method in French on the invoice
        String displayMethod = mapDBValueToDisplay(payment.getPaymentMethod());
        paymentDetails.add(new Text(displayMethod), 1, 1);

        // Signature section
        Text signatureHeaderText = createSectionHeader("SIGNATURES");

        GridPane signatureSection = new GridPane();
        signatureSection.setHgap(50);
        signatureSection.setVgap(40);

        // Create signature lines
        VBox personnelSignature = new VBox(5);
        Text personnelLabel = new Text("Personnel");
        personnelLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Separator personnelLine = new Separator();
        personnelLine.setPrefWidth(150);

        personnelSignature.getChildren().addAll(personnelLine, personnelLabel);
        personnelSignature.setAlignment(javafx.geometry.Pos.CENTER);

        VBox clientSignature = new VBox(5);
        Text clientLabel = new Text("Client");
        clientLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        Separator clientLine = new Separator();
        clientLine.setPrefWidth(150);

        clientSignature.getChildren().addAll(clientLine, clientLabel);
        clientSignature.setAlignment(javafx.geometry.Pos.CENTER);

        // Add to grid
        signatureSection.add(personnelSignature, 0, 0);
        signatureSection.add(clientSignature, 1, 0);

        // Center the signature section
        HBox signatureContainer = new HBox(signatureSection);
        signatureContainer.setAlignment(javafx.geometry.Pos.CENTER);
        signatureContainer.setPadding(new Insets(20, 0, 20, 0));

        // Footer
        Text footerText = new Text("Merci de votre confiance!\nCette facture a été générée automatiquement.");
        footerText.setFont(Font.font("Arial", 10));

        // Add all components to the invoice
        invoice.getChildren().addAll(
                headerText,
                clinicInfo,
                new Separator(),
                invoiceDetails,
                new Separator(),
                patientHeaderText,
                patientDetails,
                appointmentHeaderText,
                appointmentDetails,
                paymentHeaderText,
                paymentDetails,
                new Separator(),
                signatureHeaderText,
                signatureContainer,
                new Separator(),
                footerText
        );

        return invoice;
    }

    private Text createBoldText(String content) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        return text;
    }

    private Text createSectionHeader(String content) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        return text;
    }

    /**
     * Load payments for the payment history table (default to today's date)
     */
    private void loadPaymentHistory() {
        // Default to today's payments
        loadPaymentsByDate(LocalDate.now());
    }

    /**
     * Load payments for a specific date
     */
    private void loadPaymentsByDate(LocalDate date) {
        List<Payment> filteredPayments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Format date for SQL query
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Query to get payments for the selected date
            // FIXED: Use PaymentDate instead of PaimentDate in the SQL query
            String query = "SELECT p.*, " +
                    "pat.FNAME AS patientFirstName, pat.LNAME AS patientLastName " +
                    "FROM paiment p " +
                    "JOIN patient pat ON p.PatientID = pat.ID " +
                    "WHERE DATE(p.PaymentDate) = ? " +
                    "ORDER BY p.PaymentDate DESC";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, dateStr);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Payment payment = new Payment();

                // Use the correct column name: PaimentID for the field
                payment.setPaymentID(rs.getInt("PaimentID"));
                payment.setPatientID(rs.getString("PatientID"));
                payment.setRendezVousID(rs.getInt("RendezVousID"));
                payment.setAmount(rs.getDouble("Amount"));
                payment.setPaymentMethod(rs.getString("PaymentMethod"));
                // Use the correct column name: PaymentDate for the database column
                payment.setPaymentDate(rs.getTimestamp("PaymentDate"));

                // Set patient name
                String patientFirstName = rs.getString("patientFirstName");
                String patientLastName = rs.getString("patientLastName");
                if (patientFirstName != null && patientLastName != null) {
                    payment.setPatientName(patientFirstName + " " + patientLastName);
                }

                filteredPayments.add(payment);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement des paiements: " + e.getMessage());
        }

        if (filteredPayments.isEmpty() && !date.equals(LocalDate.now())) {
            showAlert(Alert.AlertType.INFORMATION, "Information",
                    "Aucun paiement trouvé pour cette date.");
        }

        paymentsData.setAll(filteredPayments);
    }

    /**
     * Handle date filter button click for payments
     */
    @FXML
    private void handlePaymentDateFilter(ActionEvent event) {
        LocalDate selectedDate = paymentDatePicker.getValue();
        if (selectedDate != null) {
            loadPaymentsByDate(selectedDate);
        } else {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Veuillez sélectionner une date valide.");
            // Set back to today if no date selected
            paymentDatePicker.setValue(LocalDate.now());
        }
    }

    /**
     * Handle search button click - search for completed appointments by patient CIN
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        String patientCIN = patientCINField.getText().trim();

        if (patientCIN.isEmpty()) {
            // If search field is empty, load all today's unpaid completed appointments
            loadTodayUnpaidCompletedAppointments();
            return;
        }

        List<Appointment> filteredAppointments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Get today's date in SQL format
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Updated query to filter by today's date, patient CIN, and unpaid appointments
            String query = "SELECT r.*, " +
                    "p.FNAME AS patientFirstName, p.LNAME AS patientLastName, " +
                    "u.FNAME AS doctorFirstName, u.LNAME AS doctorLastName " +
                    "FROM rendezvous r " +
                    "JOIN patient p ON r.PatientID = p.ID " +
                    "JOIN users u ON r.MedecinID = u.ID AND u.ROLE = 'medecin' " +
                    "WHERE r.Status = 'Completed' " +
                    "AND DATE(r.AppointmentDateTime) = ? " +
                    "AND r.PatientID = ? " +
                    "AND NOT EXISTS (SELECT 1 FROM paiment pay WHERE pay.RendezVousID = r.RendezVousID) " +
                    "ORDER BY r.AppointmentDateTime";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, todayStr);
            pstmt.setString(2, patientCIN);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Appointment appointment = new Appointment();

                appointment.setRendezVousID(rs.getInt("RendezVousID"));
                appointment.setPatientID(rs.getString("PatientID"));

                // Combine first and last name for patient
                String patientFirstName = rs.getString("patientFirstName");
                String patientLastName = rs.getString("patientLastName");
                appointment.setPatientName(patientFirstName + " " + patientLastName);

                appointment.setMedicinID(rs.getString("MedecinID"));

                // Combine first and last name for doctor
                String doctorFirstName = rs.getString("doctorFirstName");
                String doctorLastName = rs.getString("doctorLastName");
                appointment.setDoctorName(doctorFirstName + " " + doctorLastName);

                // Parse datetime
                Timestamp appointmentTimestamp = rs.getTimestamp("AppointmentDateTime");
                if (appointmentTimestamp != null) {
                    appointment.setAppointmentDateTime(appointmentTimestamp.toLocalDateTime());
                }

                appointment.setReasonForVisit(rs.getString("ReasonForVisit"));
                appointment.setStatus(rs.getString("Status"));

                filteredAppointments.add(appointment);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de la recherche: " + e.getMessage());
        }

        appointmentsData.setAll(filteredAppointments);

        if (filteredAppointments.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Aucun Résultat",
                    "Aucun rendez-vous terminé non payé trouvé pour ce patient aujourd'hui.");
        }
    }

    /**
     * Maps a database payment method value to a display value (French)
     */
    private String mapDBValueToDisplay(String dbValue) {
        if (dbValue == null) {
            return "Espèces"; // Default to Cash
        }

        // Clean up the value by trimming whitespace
        String cleanValue = dbValue.trim();

        if (cleanValue.equals("Cash")) {
            return "Espèces";
        } else if (cleanValue.equals("Credit Card") || cleanValue.equals(" Credit Card")) {
            return "Carte de Crédit";
        } else if (cleanValue.equals("Insurance")) {
            return "Assurance";
        }

        // Default to the original value if no match
        return dbValue;
    }

    /**
     * Maps a display value (French) to a database payment method value
     */
    private String mapDisplayToDB(String displayValue) {
        if (displayValue == null) {
            return "Cash"; // Default to Cash
        }

        // Clean up the value by trimming whitespace
        String cleanValue = displayValue.trim();

        if (cleanValue.equals("Espèces")) {
            return "Cash";
        } else if (cleanValue.equals("Carte de Crédit")) {
            return " Credit Card"; // Note the space before "Credit Card"
        } else if (cleanValue.equals("Assurance")) {
            return "Insurance";
        }

        // Default to Cash if no match
        return "Cash";
    }

    /**
     * Show payment dialog for registering a new payment
     */
    private void showPaymentDialog(Appointment appointment) {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Enregistrer un Paiement");
        dialog.setHeaderText("Patient: " + appointment.getPatientName() +
                "\nRendez-vous: " + appointment.getFormattedDateTime());

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create payment form
        VBox formLayout = new VBox(10);

        Label amountLabel = new Label("Montant:");
        TextField amountField = new TextField();
        amountField.setPromptText("Montant en DH");

        Label methodLabel = new Label("Méthode de Paiement:");

        // Use French display values in the UI
        ComboBox<String> methodComboBox = new ComboBox<>(PAYMENT_METHOD_DISPLAY_OPTIONS);
        methodComboBox.setValue("Espèces");  // Default to Cash (in French)

        formLayout.getChildren().addAll(amountLabel, amountField, methodLabel, methodComboBox);

        dialog.getDialogPane().setContent(formLayout);

        // Request focus on the amount field by default
        amountField.requestFocus();

        // Convert the result to a payment when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    String displayMethod = methodComboBox.getValue();

                    // Debug what's being selected
                    System.out.println("Selected payment method (display): " + displayMethod);

                    Payment payment = new Payment();
                    payment.setPatientID(appointment.getPatientID());
                    payment.setRendezVousID(appointment.getRendezVousID());
                    payment.setAmount(amount);

                    // Map display method to DB value
                    String dbMethod = mapDisplayToDB(displayMethod);
                    payment.setPaymentMethod(dbMethod);
                    payment.setPaymentDate(Timestamp.valueOf(LocalDateTime.now()));

                    return payment;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur de Format",
                            "Le montant doit être un nombre valide.");
                    return null;
                }
            }
            return null;
        });

        Optional<Payment> result = dialog.showAndWait();

        result.ifPresent(payment -> {
            boolean success = paymentManager.createPayment(payment);
            if (success) {
                // Set flag that we just processed a payment
                paymentJustProcessed = true;

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement enregistré avec succès.");

                // Remove this specific appointment from the table immediately
                removeAppointmentById(appointment.getRendezVousID());

                // Refresh payment history
                loadPaymentHistory();

                // Use a small delay before reloading to ensure database consistency
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.schedule(() -> {
                    Platform.runLater(() -> {
                        // Double check if we need to reload appointments
                        // Only reload if table is empty or if there's a search filter
                        if (appointmentsData.isEmpty() || !patientCINField.getText().trim().isEmpty()) {
                            String currentSearch = patientCINField.getText().trim();
                            if (currentSearch.isEmpty()) {
                                loadTodayUnpaidCompletedAppointments();
                            } else {
                                handleSearch(null);
                            }
                        }
                    });
                    executor.shutdown();
                }, 500, TimeUnit.MILLISECONDS);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'enregistrement du paiement.");
            }
        });
    }

    /**
     * Show dialog for editing an existing payment
     */
    private void showEditPaymentDialog(Payment payment) {
        // First check if payment is from today
        boolean isPaymentFromToday = false;
        if (payment.getPaymentDate() != null) {
            LocalDate paymentDate = payment.getPaymentDate().toLocalDateTime().toLocalDate();
            isPaymentFromToday = paymentDate.equals(LocalDate.now());
        }

        // Only allow editing payments from today
        if (!isPaymentFromToday) {
            showAlert(Alert.AlertType.WARNING, "Modification impossible",
                    "Seuls les paiements du jour peuvent être modifiés.");
            return;
        }

        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Modifier un Paiement");
        dialog.setHeaderText("Paiement ID: " + payment.getPaymentID());

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create payment form
        VBox formLayout = new VBox(10);

        Label amountLabel = new Label("Montant:");
        TextField amountField = new TextField(String.valueOf(payment.getAmount()));

        Label methodLabel = new Label("Méthode de Paiement:");

        // Use French display values in the UI
        ComboBox<String> methodComboBox = new ComboBox<>(PAYMENT_METHOD_DISPLAY_OPTIONS);

        // Map database value to display value for the current payment method
        String displayValue = mapDBValueToDisplay(payment.getPaymentMethod());
        methodComboBox.setValue(displayValue);

        // Add mandatory reason field for updates
        Label reasonLabel = new Label("Raison de la modification (obligatoire):");
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Expliquez pourquoi vous modifiez ce paiement");
        reasonField.setPrefRowCount(3);

        formLayout.getChildren().addAll(amountLabel, amountField, methodLabel, methodComboBox, reasonLabel, reasonField);

        dialog.getDialogPane().setContent(formLayout);

        // Request focus on the amount field by default
        amountField.requestFocus();

        // Convert the result to a payment when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Validate reason field for updates
                if (reasonField.getText() == null || reasonField.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                            "La raison de la modification est obligatoire.");
                    return null;
                }

                try {
                    double amount = Double.parseDouble(amountField.getText());
                    String displayMethod = methodComboBox.getValue();

                    // Create a copy of the original payment for history tracking
                    Payment originalPayment = new Payment();
                    originalPayment.setPaymentID(payment.getPaymentID());
                    originalPayment.setPatientID(payment.getPatientID());
                    originalPayment.setRendezVousID(payment.getRendezVousID());
                    originalPayment.setAmount(payment.getAmount());
                    originalPayment.setPaymentMethod(payment.getPaymentMethod());
                    originalPayment.setPaymentDate(payment.getPaymentDate());

                    // Update the payment with new values
                    payment.setAmount(amount);
                    // Map display method to DB value
                    String dbMethod = mapDisplayToDB(displayMethod);
                    payment.setPaymentMethod(dbMethod);

                    // Store reason and original values
                    payment.setChangeReason(reasonField.getText().trim());
                    payment.setOriginalPayment(originalPayment);

                    return payment;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur de Format",
                            "Le montant doit être un nombre valide.");
                    return null;
                }
            }
            return null;
        });

        Optional<Payment> result = dialog.showAndWait();

        result.ifPresent(updatedPayment -> {
            // Get the reason and username
            String reason = updatedPayment.getChangeReason();
            String username = UserSession.getInstance().getUsername();

            // Use updatePaymentWithHistory to track changes
            boolean success = paymentManager.updatePaymentWithHistory(
                    updatedPayment.getOriginalPayment(),
                    updatedPayment,
                    reason,
                    username
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement mis à jour avec succès.");
                loadPaymentHistory(); // Refresh payment history
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour du paiement.");
            }
        });
    }

    /**
     * Confirm and delete a payment with a mandatory reason
     */
    private void confirmDeletePayment(Payment payment) {
        // Check if payment is from today
        boolean isPaymentFromToday = false;
        if (payment.getPaymentDate() != null) {
            LocalDate paymentDate = payment.getPaymentDate().toLocalDateTime().toLocalDate();
            isPaymentFromToday = paymentDate.equals(LocalDate.now());
        }

        // Only allow deleting payments from today
        if (!isPaymentFromToday) {
            showAlert(Alert.AlertType.WARNING, "Suppression impossible",
                    "Seuls les paiements du jour peuvent être supprimés.");
            return;
        }

        // Create dialog for delete confirmation with mandatory reason
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Supprimer le paiement");
        dialog.setHeaderText("Êtes-vous sûr de vouloir supprimer ce paiement ?");

        // Set button types
        ButtonType deleteButtonType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

        // Create the reason field
        VBox content = new VBox(10);
        Label reasonLabel = new Label("Raison de la suppression (obligatoire):");
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Expliquez pourquoi vous supprimez ce paiement");
        reasonField.setPrefRowCount(3);

        content.getChildren().addAll(reasonLabel, reasonField);
        dialog.getDialogPane().setContent(content);

        // Request focus on the reason field
        Platform.runLater(() -> reasonField.requestFocus());

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == deleteButtonType) {
                String reason = reasonField.getText();

                if (reason == null || reason.trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Erreur de validation",
                            "La raison de la suppression est obligatoire.");
                    return null;
                }

                return reason.trim();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(reason -> {
            // Get username from session
            String username = UserSession.getInstance().getUsername();

            boolean success = paymentManager.deletePaymentWithHistory(
                    payment.getPaymentID(),
                    payment.getPatientID(),
                    payment.getRendezVousID(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    reason,
                    username
            );

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement supprimé avec succès.");
                loadPaymentHistory(); // Refresh payment history

                // Check if we need to reload appointments
                // Deleted payment means appointment is now unpaid again
                loadTodayUnpaidCompletedAppointments();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression du paiement.");
            }
        });
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

    /**
     * Handle back to calendar button click
     */
    @FXML
    public void handleBackToCalendar(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/appointments/CalendarView.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                    "Impossible de charger la page du calendrier: " + e.getMessage());
        }
    }
}