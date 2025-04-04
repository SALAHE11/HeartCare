package com.example.myjavafxapp.Controllers.GestionPaiement;

import javafx.geometry.Insets;
import com.example.myjavafxapp.Models.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PaymentController implements Initializable {

    @FXML private TextField patientCINField;
    @FXML private Button searchButton;

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

        // Load all payments for the payment history table
        loadPaymentHistory();
    }

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
            boolean isPaid = paymentManager.isAppointmentPaid(cellData.getValue().getRendezVousID());
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
                    boolean isPaid = paymentManager.isAppointmentPaid(appointment.getRendezVousID());

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
     * Load all payments for the payment history table
     */
    private void loadPaymentHistory() {
        List<Payment> payments = paymentManager.getAllPayments();
        paymentsData.setAll(payments);
    }

    /**
     * Handle search button click - search for completed appointments by patient CIN
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        String patientCIN = patientCINField.getText().trim();

        if (patientCIN.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Recherche Vide", "Veuillez entrer un CIN de patient.");
            return;
        }

        List<Appointment> completedAppointments = paymentManager.searchCompletedAppointmentsByPatientCIN(patientCIN);

        if (completedAppointments.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Aucun Résultat",
                    "Aucun rendez-vous terminé trouvé pour ce patient aujourd'hui.");
            appointmentsData.clear();
            return;
        }

        appointmentsData.setAll(completedAppointments);

        // Refresh the is-paid status for each appointment
        appointmentTable.refresh();
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

                    // Store the display method - it will be translated in PaymentManager
                    payment.setPaymentMethod(displayMethod);
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
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement enregistré avec succès.");
                loadPaymentHistory(); // Refresh payment history
                appointmentTable.refresh(); // Refresh appointment table to update paid status
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

                    payment.setAmount(amount);
                    payment.setPaymentMethod(displayMethod);

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
            boolean success = paymentManager.updatePayment(updatedPayment);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement mis à jour avec succès.");
                loadPaymentHistory(); // Refresh payment history
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la mise à jour du paiement.");
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

    /* Navigation Methods */

    @FXML
    public void onHome(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/CalendarView.fxml");
    }

    @FXML
    public void onCalendar(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/CalendarView.fxml");
    }

    @FXML
    public void onCreditCard(ActionEvent event) {
        // Already on this page, do nothing
    }

    @FXML
    public void onFolder(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/PatientRecordsView.fxml");
    }

    @FXML
    public void onGlobalStats(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/StatisticsDashboard.fxml");
    }

    @FXML
    public void onRepport(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/rapportQuotidien.fxml");
    }

    @FXML
    public void onBackUp(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/sauvegarde.fxml");
    }

    @FXML
    public void onUsers(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/gestionUtilisateurs.fxml");
    }

    @FXML
    public void onLogOut(ActionEvent event) {
        // Clear user session
        UserSession.getInstance().setUsername(null);
        UserSession.getInstance().setRole(null);

        navigateTo("/com/example/myjavafxapp/loginForm.fxml");
    }

    /**
     * Navigate to another view
     */
    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) patientCINField.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                    "Impossible de charger la page demandée: " + e.getMessage());
        }
    }
}