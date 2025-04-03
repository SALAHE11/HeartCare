package com.example.myjavafxapp.Controllers;

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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Models and data
    private PaymentManager paymentManager = PaymentManager.getInstance();
    private ObservableList<Appointment> appointmentsData = FXCollections.observableArrayList();
    private ObservableList<Payment> paymentsData = FXCollections.observableArrayList();

    // CRITICAL: Let's simplify things by using DIRECT database values in the UI
    // to avoid any translation problems. This is the safest approach.
    private static final ObservableList<String> PAYMENT_METHOD_OPTIONS = FXCollections.observableArrayList(
            PaymentManager.PAYMENT_METHOD_CASH,           // "Cash"
            PaymentManager.PAYMENT_METHOD_CREDIT_CARD,    // "Credit Card"
            PaymentManager.PAYMENT_METHOD_INSURANCE       // "Insurance"
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

        paymentMethodColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPaymentMethod()));

        // Set up the actions column with buttons
        paymentActionsColumn.setCellFactory(col -> new TableCell<Payment, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();

            {
                // Configure edit button
                FontIcon editIcon = new FontIcon("fas-edit");
                editIcon.setIconSize(14);
                editButton.setGraphic(editIcon);
                editButton.getStyleClass().add("icon-button");
                editButton.setTooltip(new Tooltip("Modifier paiement"));

                // Configure delete button
                FontIcon deleteIcon = new FontIcon("fas-trash");
                deleteIcon.setIconSize(14);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.getStyleClass().add("icon-button");
                deleteButton.setTooltip(new Tooltip("Supprimer paiement"));

                editButton.setOnAction(e -> {
                    Payment payment = getTableView().getItems().get(getIndex());
                    showEditPaymentDialog(payment);
                });

                deleteButton.setOnAction(e -> {
                    Payment payment = getTableView().getItems().get(getIndex());
                    showDeletePaymentConfirmation(payment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.getChildren().addAll(editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        paymentTable.setItems(paymentsData);
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

        // CRITICAL FIX: Use direct database enum values in the UI with no translation
        ComboBox<String> methodComboBox = new ComboBox<>(PAYMENT_METHOD_OPTIONS);
        methodComboBox.setValue(PaymentManager.PAYMENT_METHOD_CASH);

        formLayout.getChildren().addAll(amountLabel, amountField, methodLabel, methodComboBox);

        dialog.getDialogPane().setContent(formLayout);

        // Request focus on the amount field by default
        amountField.requestFocus();

        // Convert the result to a payment when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    String method = methodComboBox.getValue();

                    // Debug what's being selected
                    System.out.println("Selected payment method: " + method);

                    Payment payment = new Payment();
                    payment.setPatientID(appointment.getPatientID());
                    payment.setRendezVousID(appointment.getRendezVousID());
                    payment.setAmount(amount);
                    payment.setPaymentMethod(method);
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

        // CRITICAL FIX: Use direct database enum values in the UI with no translation
        ComboBox<String> methodComboBox = new ComboBox<>(PAYMENT_METHOD_OPTIONS);

        // Set current value (will fall back to Cash if not found)
        if (PAYMENT_METHOD_OPTIONS.contains(payment.getPaymentMethod())) {
            methodComboBox.setValue(payment.getPaymentMethod());
        } else {
            methodComboBox.setValue(PaymentManager.PAYMENT_METHOD_CASH);
        }

        formLayout.getChildren().addAll(amountLabel, amountField, methodLabel, methodComboBox);

        dialog.getDialogPane().setContent(formLayout);

        // Request focus on the amount field by default
        amountField.requestFocus();

        // Convert the result to a payment when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    String method = methodComboBox.getValue();

                    payment.setAmount(amount);
                    payment.setPaymentMethod(method);

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
     * Show confirmation dialog for deleting a payment
     */
    private void showDeletePaymentConfirmation(Payment payment) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de Suppression");
        alert.setHeaderText("Supprimer le Paiement ID: " + payment.getPaymentID());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce paiement?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = paymentManager.deletePayment(payment.getPaymentID());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement supprimé avec succès.");
                loadPaymentHistory(); // Refresh payment history
                appointmentTable.refresh(); // Refresh appointment table to update paid status
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression du paiement.");
            }
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