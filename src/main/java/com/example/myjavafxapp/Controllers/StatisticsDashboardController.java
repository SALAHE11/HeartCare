package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsDashboardController implements Initializable {
    // Back button
    @FXML private Button backButton;

    // Date range selection
    @FXML private ComboBox<String> periodComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button applyDateRangeButton;

    // Appointment Trends tab
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label completedAppointmentsLabel;
    @FXML private Label completedPercentLabel;
    @FXML private Label canceledAppointmentsLabel;
    @FXML private Label canceledPercentLabel;
    @FXML private Label noShowRateLabel;
    @FXML private PieChart appointmentStatusChart;
    @FXML private BarChart<String, Number> appointmentsByPeriodChart;
    @FXML private TableView<Map.Entry<String, Integer>> busiestDaysTable;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> busiestDayColumn;
    @FXML private TableColumn<Map.Entry<String, Integer>, Integer> busiestDayCountColumn;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> busiestDayPercentColumn;
    @FXML private TableView<Map.Entry<String, Integer>> busiestHoursTable;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> busiestHourColumn;
    @FXML private TableColumn<Map.Entry<String, Integer>, Integer> busiestHourCountColumn;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> busiestHourPercentColumn;

    // Patient Demographics tab
    @FXML private Label totalPatientsLabel;
    @FXML private Label newPatientsLabel;
    @FXML private Label newPatientsPercentLabel;
    @FXML private Label returningPatientsLabel;
    @FXML private Label returningPatientsPercentLabel;
    @FXML private PieChart genderDistributionChart;
    @FXML private BarChart<String, Number> ageDistributionChart;
    @FXML private TableView<DoctorPatientCount> patientsPerDoctorTable;
    @FXML private TableColumn<DoctorPatientCount, String> doctorNameColumn;
    @FXML private TableColumn<DoctorPatientCount, Integer> patientCountColumn;
    @FXML private TableColumn<DoctorPatientCount, String> percentageColumn;

    // Financial Performance tab
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgRevenuePerAppointmentLabel;
    @FXML private Label avgRevenuePerPatientLabel;
    @FXML private BarChart<String, Number> revenueTrendChart;
    @FXML private TableView<DoctorRevenue> revenueByDoctorTable;
    @FXML private TableColumn<DoctorRevenue, String> revenueDoctorColumn;
    @FXML private TableColumn<DoctorRevenue, Integer> appointmentCountColumn;
    @FXML private TableColumn<DoctorRevenue, Double> doctorRevenueColumn;
    @FXML private TableColumn<DoctorRevenue, String> revenuePercentColumn;

    // Date formatter for display
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Managers
    private AppointmentManager appointmentManager = AppointmentManager.getInstance();
    private PatientManager patientManager = PatientManager.getInstance();

    // Selected date range
    private LocalDate startDate;
    private LocalDate endDate;

    // Constants for financial calculations (since we don't have actual payment data)
    private static final double CONSULTATION_FEE = 300.0; // DH

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupDateRangeSelector();
        setupAppointmentTrendsTables();
        setupPatientsTables();
        setupFinancialTables();

        // Initial date range: last 30 days
        setDefaultDateRange();

        // Load initial data
        loadStatistics();
    }

    /**
     * Set up the date range selector with predefined periods
     */
    private void setupDateRangeSelector() {
        // Set up period combo box
        ObservableList<String> periods = FXCollections.observableArrayList(
                "Aujourd'hui",
                "Cette semaine",
                "Ce mois",
                "Cette année",
                "Les 7 derniers jours",
                "Les 30 derniers jours",
                "Les 90 derniers jours",
                "Personnalisé"
        );
        periodComboBox.setItems(periods);
        periodComboBox.setValue("Les 30 derniers jours");

        // Add listener to period combo box
        periodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals("Personnalisé")) {
                updateDateRangeBasedOnPeriod(newVal);
            }
        });
    }

    /**
     * Set up the busiest days and hours tables
     */
    private void setupAppointmentTrendsTables() {
        // Busiest days table
        busiestDayColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKey()));
        busiestDayCountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getValue()));
        busiestDayPercentColumn.setCellValueFactory(cellData -> {
            int total = getTotalAppointments();
            if (total == 0) return new SimpleStringProperty("0%");
            double percent = 100.0 * cellData.getValue().getValue() / total;
            return new SimpleStringProperty(String.format("%.1f%%", percent));
        });

        // Busiest hours table
        busiestHourColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKey()));
        busiestHourCountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getValue()));
        busiestHourPercentColumn.setCellValueFactory(cellData -> {
            int total = getTotalAppointments();
            if (total == 0) return new SimpleStringProperty("0%");
            double percent = 100.0 * cellData.getValue().getValue() / total;
            return new SimpleStringProperty(String.format("%.1f%%", percent));
        });
    }

    /**
     * Set up the patients per doctor table
     */
    private void setupPatientsTables() {
        // Patients per doctor table
        doctorNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDoctorName()));
        patientCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getPatientCount()).asObject());
        percentageColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPercentage()));
    }

    /**
     * Set up the financial tables
     */
    private void setupFinancialTables() {
        // Revenue by doctor table
        revenueDoctorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDoctorName()));
        appointmentCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getAppointmentCount()).asObject());
        doctorRevenueColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getRevenue()));
        revenuePercentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPercentage()));
    }

    /**
     * Set default date range to last 30 days
     */
    private void setDefaultDateRange() {
        startDate = LocalDate.now().minusDays(30);
        endDate = LocalDate.now();
        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);
    }

    /**
     * Update date range based on selected period
     */
    private void updateDateRangeBasedOnPeriod(String period) {
        LocalDate today = LocalDate.now();

        switch (period) {
            case "Aujourd'hui":
                startDate = today;
                endDate = today;
                break;
            case "Cette semaine":
                startDate = today.with(DayOfWeek.MONDAY);
                endDate = today;
                break;
            case "Ce mois":
                startDate = today.withDayOfMonth(1);
                endDate = today;
                break;
            case "Cette année":
                startDate = today.withDayOfYear(1);
                endDate = today;
                break;
            case "Les 7 derniers jours":
                startDate = today.minusDays(6);
                endDate = today;
                break;
            case "Les 30 derniers jours":
                startDate = today.minusDays(29);
                endDate = today;
                break;
            case "Les 90 derniers jours":
                startDate = today.minusDays(89);
                endDate = today;
                break;
            default:
                return; // No change for "Personnalisé"
        }

        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);

        // Reload statistics with new date range
        loadStatistics();
    }

    /**
     * Handle apply date range button click
     */
    @FXML
    private void handleApplyDateRange(ActionEvent event) {
        startDate = startDatePicker.getValue();
        endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showAlert(Alert.AlertType.WARNING, "Erreur de date", "Veuillez sélectionner des dates de début et de fin valides.");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showAlert(Alert.AlertType.WARNING, "Erreur de date", "La date de début doit être antérieure ou égale à la date de fin.");
            return;
        }

        // Set combo box to "Personnalisé" if dates don't match any predefined period
        periodComboBox.setValue("Personnalisé");

        // Reload statistics with new date range
        loadStatistics();
    }

    /**
     * Load all statistics
     */
    private void loadStatistics() {
        loadAppointmentStatistics();
        loadPatientDemographics();
        loadFinancialPerformance();
    }

    /**
     * Load appointment statistics
     */
    private void loadAppointmentStatistics() {
        // Get appointment statistics from AppointmentManager
        Map<String, Object> stats = appointmentManager.getAppointmentStats(startDate, endDate);

        // Update summary statistics
        int total = (int) stats.getOrDefault("total", 0);
        int completed = (int) stats.getOrDefault("completed", 0);
        int missed = (int) stats.getOrDefault("missed", 0);
        int canceled = (int) stats.getOrDefault("patientCancelled", 0) + (int) stats.getOrDefault("clinicCancelled", 0);

        totalAppointmentsLabel.setText(String.valueOf(total));
        completedAppointmentsLabel.setText(String.valueOf(completed));
        canceledAppointmentsLabel.setText(String.valueOf(canceled));

        // Calculate percentages
        double completedPercent = total > 0 ? 100.0 * completed / total : 0;
        double canceledPercent = total > 0 ? 100.0 * canceled / total : 0;
        double noShowRate = total > 0 ? 100.0 * missed / total : 0;

        completedPercentLabel.setText(String.format("(%.1f%%)", completedPercent));
        canceledPercentLabel.setText(String.format("(%.1f%%)", canceledPercent));
        noShowRateLabel.setText(String.format("%.1f%%", noShowRate));

        // Load appointment status chart
        loadAppointmentStatusChart();

        // Load appointments by period chart
        loadAppointmentsByPeriodChart();

        // Load busiest days and hours
        loadBusiestDaysAndHours();
    }

    /**
     * Load appointment status pie chart
     */
    private void loadAppointmentStatusChart() {
        // Fetch all appointments in the date range
        List<Appointment> appointments = getAppointmentsInRange();

        // Count appointments by status
        Map<String, Integer> statusCounts = new HashMap<>();

        for (Appointment appointment : appointments) {
            String status = getDisplayStatus(appointment.getStatus());
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        // Create pie chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }

        appointmentStatusChart.setData(pieChartData);
        appointmentStatusChart.setTitle("Distribution des statuts (" + appointments.size() + " rendez-vous)");
    }

    /**
     * Get user-friendly display status
     */
    private String getDisplayStatus(String status) {
        switch (status) {
            case "Scheduled": return "Programmé";
            case "CheckedIn": return "Enregistré";
            case "InProgress": return "En cours";
            case "Completed": return "Terminé";
            case "Missed": return "Manqué";
            case "Patient_Cancelled": return "Annulé (Patient)";
            case "Clinic_Cancelled": return "Annulé (Clinique)";
            case "Rescheduled": return "Reprogrammé";
            default: return status;
        }
    }

    /**
     * Load appointments by period chart
     */
    private void loadAppointmentsByPeriodChart() {
        // Fetch all appointments in the date range
        List<Appointment> appointments = getAppointmentsInRange();

        // Clear existing data
        appointmentsByPeriodChart.getData().clear();

        // Determine period type based on date range
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        if (daysBetween <= 7) {
            // Daily data
            loadDailyAppointmentChart(appointments);
        } else if (daysBetween <= 90) {
            // Weekly data
            loadWeeklyAppointmentChart(appointments);
        } else {
            // Monthly data
            loadMonthlyAppointmentChart(appointments);
        }
    }

    /**
     * Load daily appointment chart
     */
    private void loadDailyAppointmentChart(List<Appointment> appointments) {
        Map<LocalDate, Integer> dailyCounts = new TreeMap<>();

        // Initialize all dates in the range with zero
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyCounts.put(currentDate, 0);
            currentDate = currentDate.plusDays(1);
        }

        // Count appointments by date
        for (Appointment appointment : appointments) {
            LocalDate appDate = appointment.getAppointmentDateTime().toLocalDate();
            dailyCounts.put(appDate, dailyCounts.getOrDefault(appDate, 0) + 1);
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rendez-vous par jour");

        for (Map.Entry<LocalDate, Integer> entry : dailyCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(dateFormatter),
                    entry.getValue()
            ));
        }

        appointmentsByPeriodChart.getData().add(series);
        appointmentsByPeriodChart.setTitle("Rendez-vous par jour");
    }

    /**
     * Load weekly appointment chart
     */
    private void loadWeeklyAppointmentChart(List<Appointment> appointments) {
        Map<String, Integer> weeklyCounts = new LinkedHashMap<>();

        // Group appointments by week
        for (Appointment appointment : appointments) {
            LocalDate appDate = appointment.getAppointmentDateTime().toLocalDate();
            LocalDate weekStart = appDate.with(DayOfWeek.MONDAY);
            String weekLabel = weekStart.format(dateFormatter);
            weeklyCounts.put(weekLabel, weeklyCounts.getOrDefault(weekLabel, 0) + 1);
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rendez-vous par semaine");

        for (Map.Entry<String, Integer> entry : weeklyCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    "Sem. " + entry.getKey(),
                    entry.getValue()
            ));
        }

        appointmentsByPeriodChart.getData().add(series);
        appointmentsByPeriodChart.setTitle("Rendez-vous par semaine");
    }

    /**
     * Load monthly appointment chart
     */
    private void loadMonthlyAppointmentChart(List<Appointment> appointments) {
        Map<String, Integer> monthlyCounts = new LinkedHashMap<>();

        // Group appointments by month
        for (Appointment appointment : appointments) {
            LocalDate appDate = appointment.getAppointmentDateTime().toLocalDate();
            String monthKey = appDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    + " " + appDate.getYear();
            monthlyCounts.put(monthKey, monthlyCounts.getOrDefault(monthKey, 0) + 1);
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rendez-vous par mois");

        for (Map.Entry<String, Integer> entry : monthlyCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        appointmentsByPeriodChart.getData().add(series);
        appointmentsByPeriodChart.setTitle("Rendez-vous par mois");
    }

    /**
     * Load busiest days and hours
     */
    private void loadBusiestDaysAndHours() {
        // Fetch all appointments in the date range
        List<Appointment> appointments = getAppointmentsInRange();

        // Count appointments by day of week
        Map<String, Integer> dayOfWeekCounts = new LinkedHashMap<>();

        // Initialize days of week with zero counts
        for (DayOfWeek day : DayOfWeek.values()) {
            dayOfWeekCounts.put(
                    day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    0
            );
        }

        // Count appointments by day of week
        for (Appointment appointment : appointments) {
            LocalDateTime appDateTime = appointment.getAppointmentDateTime();
            String dayOfWeek = appDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
            dayOfWeekCounts.put(dayOfWeek, dayOfWeekCounts.getOrDefault(dayOfWeek, 0) + 1);
        }

        // Sort by count descending
        List<Map.Entry<String, Integer>> sortedDays = dayOfWeekCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Update busiest days table
        busiestDaysTable.setItems(FXCollections.observableArrayList(sortedDays));

        // Count appointments by hour
        Map<String, Integer> hourCounts = new TreeMap<>();

        // Initialize hours with zero counts
        for (int hour = 8; hour <= 17; hour++) {
            hourCounts.put(String.format("%02d:00", hour), 0);
            hourCounts.put(String.format("%02d:30", hour), 0);
        }

        // Count appointments by hour
        for (Appointment appointment : appointments) {
            LocalDateTime appDateTime = appointment.getAppointmentDateTime();
            String hourKey = String.format("%02d:%02d", appDateTime.getHour(), appDateTime.getMinute() / 30 * 30);
            hourCounts.put(hourKey, hourCounts.getOrDefault(hourKey, 0) + 1);
        }

        // Sort by count descending
        List<Map.Entry<String, Integer>> sortedHours = hourCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Update busiest hours table
        busiestHoursTable.setItems(FXCollections.observableArrayList(sortedHours));
    }

    /**
     * Load patient demographics
     */
    private void loadPatientDemographics() {
        // Calculate total, new, and returning patients
        List<Patient> allPatients = patientManager.getAllPatients();

        // Count patients created in the date range as "new"
        int newPatients = getNewPatientsCount();
        int totalPatients = allPatients.size();
        int returningPatients = totalPatients - newPatients;

        // Update labels
        totalPatientsLabel.setText(String.valueOf(totalPatients));
        newPatientsLabel.setText(String.valueOf(newPatients));
        returningPatientsLabel.setText(String.valueOf(returningPatients));

        // Calculate percentages
        double newPercent = totalPatients > 0 ? 100.0 * newPatients / totalPatients : 0;
        double returningPercent = totalPatients > 0 ? 100.0 * returningPatients / totalPatients : 0;

        newPatientsPercentLabel.setText(String.format("(%.1f%%)", newPercent));
        returningPatientsPercentLabel.setText(String.format("(%.1f%%)", returningPercent));

        // Load gender distribution chart
        loadGenderDistributionChart(allPatients);

        // Load age distribution chart
        loadAgeDistributionChart(allPatients);

        // Load patients per doctor
        loadPatientsPerDoctor();
    }

    /**
     * Get count of new patients created in the date range
     */
    private int getNewPatientsCount() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        int count = 0;

        try {
            String query = "SELECT COUNT(*) FROM patient WHERE CREATED_AT BETWEEN ? AND ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * Load gender distribution chart
     */
    private void loadGenderDistributionChart(List<Patient> patients) {
        // Count patients by gender
        int maleCount = 0;
        int femaleCount = 0;

        for (Patient patient : patients) {
            if ("Male".equalsIgnoreCase(patient.getSEXE())) {
                maleCount++;
            } else if ("Female".equalsIgnoreCase(patient.getSEXE())) {
                femaleCount++;
            }
        }

        // Create pie chart data
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Hommes (" + maleCount + ")", maleCount),
                new PieChart.Data("Femmes (" + femaleCount + ")", femaleCount)
        );

        genderDistributionChart.setData(pieChartData);
        genderDistributionChart.setTitle("Distribution par sexe (" + patients.size() + " patients)");
    }

    /**
     * Load age distribution chart
     */
    private void loadAgeDistributionChart(List<Patient> patients) {
        // Group patients by age range
        Map<String, Integer> ageGroups = new LinkedHashMap<>();

        // Initialize age groups
        ageGroups.put("0-10", 0);
        ageGroups.put("11-20", 0);
        ageGroups.put("21-30", 0);
        ageGroups.put("31-40", 0);
        ageGroups.put("41-50", 0);
        ageGroups.put("51-60", 0);
        ageGroups.put("61-70", 0);
        ageGroups.put("71+", 0);

        // Calculate age for each patient and count by group
        LocalDate now = LocalDate.now();

        for (Patient patient : patients) {
            try {
                LocalDate birthdate = LocalDate.parse(patient.getBIRTHDATE());
                int age = (int) ChronoUnit.YEARS.between(birthdate, now);

                if (age <= 10) {
                    ageGroups.put("0-10", ageGroups.get("0-10") + 1);
                } else if (age <= 20) {
                    ageGroups.put("11-20", ageGroups.get("11-20") + 1);
                } else if (age <= 30) {
                    ageGroups.put("21-30", ageGroups.get("21-30") + 1);
                } else if (age <= 40) {
                    ageGroups.put("31-40", ageGroups.get("31-40") + 1);
                } else if (age <= 50) {
                    ageGroups.put("41-50", ageGroups.get("41-50") + 1);
                } else if (age <= 60) {
                    ageGroups.put("51-60", ageGroups.get("51-60") + 1);
                } else if (age <= 70) {
                    ageGroups.put("61-70", ageGroups.get("61-70") + 1);
                } else {
                    ageGroups.put("71+", ageGroups.get("71+") + 1);
                }
            } catch (Exception e) {
                // Skip invalid birthdate
            }
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de patients");

        for (Map.Entry<String, Integer> entry : ageGroups.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        // Clear existing data and add new series
        ageDistributionChart.getData().clear();
        ageDistributionChart.getData().add(series);
        ageDistributionChart.setTitle("Distribution par âge");
    }

    /**
     * Load patients per doctor
     */
    private void loadPatientsPerDoctor() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Get count of unique patients seen by each doctor
            String query =
                    "SELECT u.ID, u.FNAME, u.LNAME, COUNT(DISTINCT r.PatientID) as patientCount " +
                            "FROM users u " +
                            "JOIN rendezvous r ON u.ID = r.MedecinID " +
                            "WHERE u.ROLE = 'medecin' " +
                            "AND r.AppointmentDateTime BETWEEN ? AND ? " +
                            "GROUP BY u.ID " +
                            "ORDER BY patientCount DESC";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            ResultSet rs = pstmt.executeQuery();

            ObservableList<DoctorPatientCount> doctorPatients = FXCollections.observableArrayList();
            int totalPatients = 0;

            while (rs.next()) {
                String doctorName = "Dr. " + rs.getString("LNAME") + ", " + rs.getString("FNAME");
                int patientCount = rs.getInt("patientCount");
                totalPatients += patientCount;

                doctorPatients.add(new DoctorPatientCount(doctorName, patientCount));
            }

            // Calculate percentages
            for (DoctorPatientCount doctorPatient : doctorPatients) {
                double percent = totalPatients > 0 ? 100.0 * doctorPatient.getPatientCount() / totalPatients : 0;
                doctorPatient.setPercentage(String.format("%.1f%%", percent));
            }

            patientsPerDoctorTable.setItems(doctorPatients);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load financial performance
     */
    private void loadFinancialPerformance() {
        // Get all completed appointments in date range
        List<Appointment> completedAppointments = getCompletedAppointments();

        // Calculate total revenue (assuming a fixed fee per appointment)
        double totalRevenue = completedAppointments.size() * CONSULTATION_FEE;

        // Calculate unique patient count
        Set<String> uniquePatients = new HashSet<>();
        for (Appointment appointment : completedAppointments) {
            uniquePatients.add(appointment.getPatientID());
        }
        int uniquePatientCount = uniquePatients.size();

        // Calculate average revenue per appointment and per patient
        double avgRevenuePerAppointment = completedAppointments.isEmpty() ? 0 : totalRevenue / completedAppointments.size();
        double avgRevenuePerPatient = uniquePatientCount == 0 ? 0 : totalRevenue / uniquePatientCount;

        // Update labels
        totalRevenueLabel.setText(String.format("%.2f DH", totalRevenue));
        avgRevenuePerAppointmentLabel.setText(String.format("%.2f DH", avgRevenuePerAppointment));
        avgRevenuePerPatientLabel.setText(String.format("%.2f DH", avgRevenuePerPatient));

        // Load revenue trend chart
        loadRevenueTrendChart(completedAppointments);

        // Load revenue by doctor
        loadRevenueByDoctor(completedAppointments);
    }

    /**
     * Load revenue trend chart
     */
    private void loadRevenueTrendChart(List<Appointment> appointments) {
        // Determine period type based on date range
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

        if (daysBetween <= 7) {
            // Daily data
            loadDailyRevenueChart(appointments);
        } else if (daysBetween <= 90) {
            // Weekly data
            loadWeeklyRevenueChart(appointments);
        } else {
            // Monthly data
            loadMonthlyRevenueChart(appointments);
        }
    }

    /**
     * Load daily revenue chart
     */
    private void loadDailyRevenueChart(List<Appointment> appointments) {
        Map<LocalDate, Double> dailyRevenue = new TreeMap<>();

        // Initialize all dates in the range with zero
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailyRevenue.put(currentDate, 0.0);
            currentDate = currentDate.plusDays(1);
        }

        // Calculate revenue by date
        for (Appointment appointment : appointments) {
            LocalDate appDate = appointment.getAppointmentDateTime().toLocalDate();
            dailyRevenue.put(appDate, dailyRevenue.getOrDefault(appDate, 0.0) + CONSULTATION_FEE);
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenu par jour");

        for (Map.Entry<LocalDate, Double> entry : dailyRevenue.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    entry.getKey().format(dateFormatter),
                    entry.getValue()
            ));
        }

        // Clear existing data and add new series
        revenueTrendChart.getData().clear();
        revenueTrendChart.getData().add(series);
        revenueTrendChart.setTitle("Revenu par jour");
    }

    /**
     * Load weekly revenue chart
     */
    private void loadWeeklyRevenueChart(List<Appointment> appointments) {
        Map<String, Double> weeklyRevenue = new LinkedHashMap<>();

        // Group appointments by week
        for (Appointment appointment : appointments) {
            LocalDate appDate = appointment.getAppointmentDateTime().toLocalDate();
            LocalDate weekStart = appDate.with(DayOfWeek.MONDAY);
            String weekLabel = weekStart.format(dateFormatter);
            weeklyRevenue.put(weekLabel, weeklyRevenue.getOrDefault(weekLabel, 0.0) + CONSULTATION_FEE);
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenu par semaine");

        for (Map.Entry<String, Double> entry : weeklyRevenue.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    "Sem. " + entry.getKey(),
                    entry.getValue()
            ));
        }

        // Clear existing data and add new series
        revenueTrendChart.getData().clear();
        revenueTrendChart.getData().add(series);
        revenueTrendChart.setTitle("Revenu par semaine");
    }

    /**
     * Load monthly revenue chart
     */
    private void loadMonthlyRevenueChart(List<Appointment> appointments) {
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();

        // Group appointments by month
        for (Appointment appointment : appointments) {
            LocalDate appDate = appointment.getAppointmentDateTime().toLocalDate();
            String monthKey = appDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    + " " + appDate.getYear();
            monthlyRevenue.put(monthKey, monthlyRevenue.getOrDefault(monthKey, 0.0) + CONSULTATION_FEE);
        }

        // Create chart series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenu par mois");

        for (Map.Entry<String, Double> entry : monthlyRevenue.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        // Clear existing data and add new series
        revenueTrendChart.getData().clear();
        revenueTrendChart.getData().add(series);
        revenueTrendChart.setTitle("Revenu par mois");
    }

    /**
     * Load revenue by doctor
     */
    private void loadRevenueByDoctor(List<Appointment> appointments) {
        // Group appointments by doctor
        Map<String, DoctorRevenue> doctorRevenueMap = new HashMap<>();

        for (Appointment appointment : appointments) {
            String doctorId = appointment.getMedicinID();
            String doctorName = appointment.getDoctorName();

            if (doctorName == null || doctorName.isEmpty()) {
                // Try to look up doctor name if not available in appointment
                doctorName = getDoctorName(doctorId);
            }

            if (doctorName == null || doctorName.isEmpty()) {
                doctorName = "Dr. " + doctorId;
            } else if (!doctorName.startsWith("Dr.")) {
                doctorName = "Dr. " + doctorName;
            }

            DoctorRevenue doctorRevenue = doctorRevenueMap.getOrDefault(
                    doctorId,
                    new DoctorRevenue(doctorName, 0, 0.0)
            );

            doctorRevenue.incrementAppointmentCount();
            doctorRevenue.addRevenue(CONSULTATION_FEE);

            doctorRevenueMap.put(doctorId, doctorRevenue);
        }

        // Calculate total revenue
        double totalRevenue = appointments.size() * CONSULTATION_FEE;

        // Calculate percentages
        for (DoctorRevenue doctorRevenue : doctorRevenueMap.values()) {
            double percent = totalRevenue > 0 ? 100.0 * doctorRevenue.getRevenue() / totalRevenue : 0;
            doctorRevenue.setPercentage(String.format("%.1f%%", percent));
        }

        // Convert to list and sort by revenue
        List<DoctorRevenue> doctorRevenueList = new ArrayList<>(doctorRevenueMap.values());
        doctorRevenueList.sort(Comparator.comparing(DoctorRevenue::getRevenue).reversed());

        // Update table
        revenueByDoctorTable.setItems(FXCollections.observableArrayList(doctorRevenueList));
    }

    /**
     * Get doctor name from ID
     */
    private String getDoctorName(String doctorId) {
        if (doctorId == null) return "";

        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT FNAME, LNAME FROM users WHERE ID = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, doctorId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("LNAME") + ", " + rs.getString("FNAME");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Get all appointments in the selected date range
     */
    private List<Appointment> getAppointmentsInRange() {
        // Create a list to hold all appointment from all days in the range
        List<Appointment> allAppointments = new ArrayList<>();

        // Fetch appointments for each day in the range
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Appointment> dayAppointments = appointmentManager.getAppointmentsByDate(currentDate);
            allAppointments.addAll(dayAppointments);
            currentDate = currentDate.plusDays(1);
        }

        return allAppointments;
    }

    /**
     * Get all completed appointments in the selected date range
     */
    private List<Appointment> getCompletedAppointments() {
        // Get all appointments and filter by status
        return getAppointmentsInRange().stream()
                .filter(a -> "Completed".equals(a.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get total number of appointments
     */
    private int getTotalAppointments() {
        return getAppointmentsInRange().size();
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

    /* Helper classes for table data */

    /**
     * Helper class for doctor patient count
     */
    public static class DoctorPatientCount {
        private String doctorName;
        private int patientCount;
        private String percentage;

        public DoctorPatientCount(String doctorName, int patientCount) {
            this.doctorName = doctorName;
            this.patientCount = patientCount;
            this.percentage = "0%";
        }

        public String getDoctorName() {
            return doctorName;
        }

        public int getPatientCount() {
            return patientCount;
        }

        public String getPercentage() {
            return percentage;
        }

        public void setPercentage(String percentage) {
            this.percentage = percentage;
        }
    }

    /**
     * Helper class for doctor revenue
     */
    public static class DoctorRevenue {
        private String doctorName;
        private int appointmentCount;
        private double revenue;
        private String percentage;

        public DoctorRevenue(String doctorName, int appointmentCount, double revenue) {
            this.doctorName = doctorName;
            this.appointmentCount = appointmentCount;
            this.revenue = revenue;
            this.percentage = "0%";
        }

        public String getDoctorName() {
            return doctorName;
        }

        public int getAppointmentCount() {
            return appointmentCount;
        }

        public double getRevenue() {
            return revenue;
        }

        public String getPercentage() {
            return percentage;
        }

        public void setPercentage(String percentage) {
            this.percentage = percentage;
        }

        public void incrementAppointmentCount() {
            this.appointmentCount++;
        }

        public void addRevenue(double amount) {
            this.revenue += amount;
        }
    }

    /**
     * Handle back button click to return to the dashboard
     */
    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        navigateTo("/com/example/myjavafxapp/CalendarView.fxml");
    }

    /**
     * Helper method to navigate to another view
     */
    private void navigateTo(String fxmlPath) {
        try {
            // Load the view
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Get the current stage
            Stage currentStage = (Stage) totalAppointmentsLabel.getScene().getWindow();

            // Set the new scene
            currentStage.setScene(new Scene(loader.load()));

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", "Impossible de charger la page demandée");
        }
    }
}