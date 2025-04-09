package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.DailyReport;
import com.example.myjavafxapp.Models.DailyReportManager;
import com.example.myjavafxapp.Models.SwitchScene;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Daily Report view
 */
public class RapportQuotidienController implements Initializable {

    @FXML private DatePicker reportDatePicker;
    @FXML private Label partialReportWarningLabel;
    @FXML private Button generateReportButton;
    @FXML private Button backButton;

    // Summary section
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label completedAppointmentsLabel;
    @FXML private Label canceledAppointmentsLabel;
    @FXML private Label patientsSeenLabel;

    // Patient list table
    @FXML private TableView<DailyReport.PatientVisit> patientsTableView;
    @FXML private TableColumn<DailyReport.PatientVisit, String> cinColumn;
    @FXML private TableColumn<DailyReport.PatientVisit, String> prenomColumn;
    @FXML private TableColumn<DailyReport.PatientVisit, String> nomColumn;
    @FXML private TableColumn<DailyReport.PatientVisit, String> heureColumn;
    @FXML private TableColumn<DailyReport.PatientVisit, String> paymentMethodColumn;

    // Peak hours section
    @FXML private Label peakHourLabel;

    // Hourly appointments table
    @FXML private TableView<Map.Entry<LocalTime, Integer>> hourlyAppointmentsTable;
    @FXML private TableColumn<Map.Entry<LocalTime, Integer>, String> hourColumn;
    @FXML private TableColumn<Map.Entry<LocalTime, Integer>, Integer> appointmentCountColumn;

    // Revenue section
    @FXML private Label totalRevenueLabel;
    @FXML private VBox paymentMethodsVBox;

    // Staff performance section
    @FXML private VBox doctorPerformanceVBox;

    private DailyReportManager reportManager = DailyReportManager.getInstance();
    private DailyReport currentReport;

    // Date/time formatters
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up table columns
        setupTableColumns();

        // Set default date to today
        reportDatePicker.setValue(LocalDate.now());

        // Add listener to date picker
        reportDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadReport(newVal);
        });

        // Initial load
        loadReport(LocalDate.now());
    }

    /**
     * Set up table columns for the patients list and hourly appointments
     */
    private void setupTableColumns() {
        // Patient list table
        cinColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCin()));
        prenomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFirstName()));
        nomColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLastName()));
        heureColumn.setCellValueFactory(data -> {
            LocalDateTime time = data.getValue().getVisitTime();
            return new SimpleStringProperty(time.format(timeFormatter));
        });
        paymentMethodColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPaymentMethod()));

        // Hourly appointments table
        hourColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getKey().format(timeFormatter)));
        appointmentCountColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getValue()).asObject());
    }

    /**
     * Load report data for the specified date
     */
    private void loadReport(LocalDate date) {
        currentReport = reportManager.generateDailyReport(date);
        updateUI();
    }

    /**
     * Update UI with current report data
     */
    private void updateUI() {
        // Check if report is partial
        partialReportWarningLabel.setVisible(currentReport.isPartial());

        // Update appointment summary
        totalAppointmentsLabel.setText(String.valueOf(currentReport.getTotalAppointments()));
        completedAppointmentsLabel.setText(String.valueOf(currentReport.getCompletedAppointments()));
        canceledAppointmentsLabel.setText(String.valueOf(currentReport.getCanceledAppointments()));

        // Update patients seen
        if (currentReport.getPatientsVisits() != null) {
            patientsSeenLabel.setText(String.valueOf(currentReport.getPatientsVisits().size()));
            patientsTableView.setItems(FXCollections.observableArrayList(currentReport.getPatientsVisits()));
        } else {
            patientsSeenLabel.setText("0");
            patientsTableView.getItems().clear();
        }

        // Update peak hour
        LocalTime peakHour = currentReport.getPeakHour();
        if (peakHour != null) {
            peakHourLabel.setText(peakHour.format(timeFormatter));
        } else {
            peakHourLabel.setText("N/A");
        }

        // Update hourly appointments table
        // Filter to include only hours with at least one appointment and sort by time
        List<Map.Entry<LocalTime, Integer>> hourlyAppointments = currentReport.getAppointmentsByHour().entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= 1)
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        hourlyAppointmentsTable.setItems(FXCollections.observableArrayList(hourlyAppointments));

        // Update revenue
        totalRevenueLabel.setText(String.format("%.2f DH", currentReport.getTotalRevenue()));

        // Update payment methods
        paymentMethodsVBox.getChildren().clear();
        for (Map.Entry<String, Double> entry : currentReport.getRevenueByPaymentMethod().entrySet()) {
            Label label = new Label(entry.getKey() + ": " + String.format("%.2f DH", entry.getValue()));
            paymentMethodsVBox.getChildren().add(label);
        }

        // Update doctor performance
        doctorPerformanceVBox.getChildren().clear();
        for (Map.Entry<String, Integer> entry : currentReport.getPatientsByDoctor().entrySet()) {
            Label label = new Label(entry.getKey() + ": " + entry.getValue() + " patients");
            doctorPerformanceVBox.getChildren().add(label);
        }
    }

    /**
     * Generate and download PDF report
     */
    @FXML
    private void handleGenerateReport() {
        // Choose directory to save the report
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Sélectionner le dossier pour enregistrer le rapport");
        File selectedDirectory = directoryChooser.showDialog(generateReportButton.getScene().getWindow());

        if (selectedDirectory != null) {
            generatePDF(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Generate PDF and save to the specified directory
     */
    private void generatePDF(String directory) {
        try {
            // Create PDF document
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Create content stream
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = 750; // Start position from top
            float margin = 50;
            float indent = 20;

            // Set font for title
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);

            // Add title
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Rapport Quotidien - " + currentReport.getReportDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            contentStream.endText();
            yPosition -= 25;

            // Add partial report warning if needed
            if (currentReport.isPartial()) {
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Attention : Ce rapport est partiel car la journée n'est pas terminée.");
                contentStream.endText();
                yPosition -= 25;
            }

            // Add appointment summary
            yPosition -= 15;
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Résumé des Rendez-vous");
            contentStream.endText();
            yPosition -= 20;

            contentStream.setFont(PDType1Font.HELVETICA, 12);

            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Nombre total de rendez-vous : " + currentReport.getTotalAppointments());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Rendez-vous terminés : " + currentReport.getCompletedAppointments());
            contentStream.endText();
            yPosition -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Rendez-vous annulés : " + currentReport.getCanceledAppointments());
            contentStream.endText();
            yPosition -= 20;

            int patientsSeen = (currentReport.getPatientsVisits() != null) ?
                    currentReport.getPatientsVisits().size() : 0;

            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Patients vus : " + patientsSeen);
            contentStream.endText();
            yPosition -= 30;

            // Add patient list
            if (patientsSeen > 0) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin + indent, yPosition);
                contentStream.showText("Liste des patients vus :");
                contentStream.endText();
                yPosition -= 20;

                contentStream.setFont(PDType1Font.HELVETICA, 10);

                // Create table headers
                float tableWidth = 450;
                float[] columnWidths = {80, 90, 90, 80, 110}; // CIN, Prénom, Nom, Heure, Méthode de paiement
                float tableStartX = margin + indent;
                float tableStartY = yPosition;
                float rowHeight = 20;

                // Draw table headers
                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX, tableStartY);
                contentStream.showText("CIN");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + columnWidths[0], tableStartY);
                contentStream.showText("Prénom");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + columnWidths[0] + columnWidths[1], tableStartY);
                contentStream.showText("Nom");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + columnWidths[0] + columnWidths[1] + columnWidths[2], tableStartY);
                contentStream.showText("Heure");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(tableStartX + columnWidths[0] + columnWidths[1] + columnWidths[2] + columnWidths[3], tableStartY);
                contentStream.showText("Paiement");
                contentStream.endText();

                yPosition -= rowHeight;

                // Draw patient rows
                for (DailyReport.PatientVisit visit : currentReport.getPatientsVisits()) {
                    // Check if we need to add a new page
                    if (yPosition < 100) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(PDType1Font.HELVETICA, 10);
                        yPosition = 750;
                    }

                    // Draw row data
                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX, yPosition);
                    contentStream.showText(visit.getCin());
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + columnWidths[0], yPosition);
                    contentStream.showText(visit.getFirstName());
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + columnWidths[0] + columnWidths[1], yPosition);
                    contentStream.showText(visit.getLastName());
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + columnWidths[0] + columnWidths[1] + columnWidths[2], yPosition);
                    contentStream.showText(visit.getVisitTime().format(timeFormatter));
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableStartX + columnWidths[0] + columnWidths[1] + columnWidths[2] + columnWidths[3], yPosition);
                    contentStream.showText(visit.getPaymentMethod());
                    contentStream.endText();

                    yPosition -= rowHeight;
                }
            }

            // Check if we need to add a new page
            if (yPosition < 200) {
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                yPosition = 750;
            } else {
                yPosition -= 30;
            }

            // Add peak hour and hourly distribution
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Flux des Patients");
            contentStream.endText();
            yPosition -= 20;

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            LocalTime peakHour = currentReport.getPeakHour();
            if (peakHour != null) {
                contentStream.showText("Heure de pointe : " + peakHour.format(timeFormatter));
            } else {
                contentStream.showText("Heure de pointe : N/A");
            }
            contentStream.endText();
            yPosition -= 25;

            // Add hourly distribution table
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Répartition des RDV par heure :");
            contentStream.endText();
            yPosition -= 20;

            // Table headers for hourly distribution
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
            float hourlyTableStartX = margin + indent;
            float hourlyTableStartY = yPosition;
            float[] hourlyColumnWidths = {100, 100};

            contentStream.beginText();
            contentStream.newLineAtOffset(hourlyTableStartX, hourlyTableStartY);
            contentStream.showText("Heure");
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(hourlyTableStartX + hourlyColumnWidths[0], hourlyTableStartY);
            contentStream.showText("Nombre de RDV");
            contentStream.endText();

            yPosition -= 15;

            // Table rows for hourly distribution
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            List<Map.Entry<LocalTime, Integer>> hourlyAppointments = currentReport.getAppointmentsByHour().entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() >= 1)
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            for (Map.Entry<LocalTime, Integer> entry : hourlyAppointments) {
                // Check if we need a new page
                if (yPosition < 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    yPosition = 750;
                }

                contentStream.beginText();
                contentStream.newLineAtOffset(hourlyTableStartX, yPosition);
                contentStream.showText(entry.getKey().format(timeFormatter));
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(hourlyTableStartX + hourlyColumnWidths[0], yPosition);
                contentStream.showText(String.valueOf(entry.getValue()));
                contentStream.endText();

                yPosition -= 15;
            }

            yPosition -= 20;

            // Add revenue summary
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Résumé des Revenus");
            contentStream.endText();
            yPosition -= 20;

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Revenu total : " + String.format("%.2f DH", currentReport.getTotalRevenue()));
            contentStream.endText();
            yPosition -= 20;

            // Add payment methods
            contentStream.beginText();
            contentStream.newLineAtOffset(margin + indent, yPosition);
            contentStream.showText("Répartition par méthode de paiement :");
            contentStream.endText();
            yPosition -= 20;

            for (Map.Entry<String, Double> entry : currentReport.getRevenueByPaymentMethod().entrySet()) {
                contentStream.beginText();
                contentStream.newLineAtOffset(margin + indent * 2, yPosition);
                contentStream.showText(entry.getKey() + ": " + String.format("%.2f DH", entry.getValue()));
                contentStream.endText();
                yPosition -= 20;

                // Add new page if needed
                if (yPosition < 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    yPosition = 750;
                }
            }

            yPosition -= 15;

            // Add staff performance
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Performance du Personnel");
            contentStream.endText();
            yPosition -= 20;

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            for (Map.Entry<String, Integer> entry : currentReport.getPatientsByDoctor().entrySet()) {
                contentStream.beginText();
                contentStream.newLineAtOffset(margin + indent, yPosition);
                contentStream.showText(entry.getKey() + ": " + entry.getValue() + " patients");
                contentStream.endText();
                yPosition -= 20;

                // Add new page if needed
                if (yPosition < 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    yPosition = 750;
                }
            }

            // Close content stream
            contentStream.close();

            // Save document
            String fileName = "RapportQuotidien_" + currentReport.getReportDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            String filePath = directory + File.separator + fileName;
            document.save(filePath);
            document.close();

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le rapport a été enregistré avec succès dans :\n" + filePath);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    /**
     * Navigate back to the calendar view
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            SwitchScene.switchScene(event, "/com/example/myjavafxapp/CalendarView.fxml");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à l'écran précédent : " + e.getMessage());
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