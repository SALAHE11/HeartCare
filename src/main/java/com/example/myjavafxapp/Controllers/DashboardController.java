package com.example.myjavafxapp.Controllers;

import com.example.myjavafxapp.Models.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    @FXML
    public Button homeButton;

    @FXML
    public Label welcomeMessage;
    @FXML
    public Label currentTime;
    @FXML
    public Label totalAppointments;
    @FXML
    public Label noShowRate;
    @FXML
    public Label registeredPatients;
    @FXML
    public Label upComingClientName;
    @FXML
    public Label upComingTime;
    @FXML
    public Label missedClientName;
    @FXML
    public Label missedTime;
    @FXML
    public Button gestionRendezVous;
    @FXML
    public Button gestionPaiment;
    @FXML
    public Button dossierPatient;
    @FXML
    public Button statistiqueGlobales;
    @FXML
    public Button rapportQuotidien;
    @FXML
    public Button sauvegarde;
    @FXML
    public Button gestionUtilisateur;
    @FXML
    public Button disconnect;
    @FXML
    private BarChart<String, Double> chart;

    private Timeline timeline;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up real-time updates
        setupRealTimeUpdates();

        // Initial data fetch
        refreshDashboardData();

        // Set up button visibility based on user role
        setupButtonVisibility();

        // Set up welcome message
        setupWelcomeMessage();
    }

    private void setupWelcomeMessage() {
        welcomeMessage.setText("Bienvenu " + UserSession.getInstance().getUsername() + " !");
    }

    private void setupRealTimeUpdates() {
        // Update time every second
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTime()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Refresh dashboard data every minute
        Timeline dataRefreshTimeline = new Timeline(new KeyFrame(Duration.minutes(1), event -> refreshDashboardData()));
        dataRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        dataRefreshTimeline.play();
    }

    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd MMMM yyyy");
        currentTime.setText(formatter.format(now));
    }

    private void refreshDashboardData() {
        // Fetch statistics from the database (based on today's data)
        Statistics stats = fetchStatistics();
        if (stats != null) {
            totalAppointments.setText(String.valueOf(stats.getTotalAppointments()));
            // Format the no-show rate as a percentage
            noShowRate.setText(String.format("%.1f%%", stats.getNoShowRate() * 100));
            registeredPatients.setText(String.valueOf(stats.getRegisteredPatients()));
        }

        // Fetch upcoming and missed appointments (restricted to today)
        fetchAppointments();

        // Update chart with today’s appointment data
        updateChart();
    }

    private Statistics fetchStatistics() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        Statistics stats = null;

        try {
            // Statistics for the current day:
            // - total appointments today
            // - no-show rate: cancelled appointments / total appointments (guarding against division by zero)
            // - registered patients today (using 'created_at' as the registration date column)
            String sql = "SELECT " +
                    "(SELECT COUNT(*) FROM rendezvous WHERE DATE(appointmentdatetime) = CURDATE()) AS total_appointments, " +
                    "(SELECT IF((SELECT COUNT(*) FROM rendezvous WHERE DATE(appointmentdatetime) = CURDATE()) = 0, 0, " +
                    "(SELECT COUNT(*) FROM rendezvous WHERE status = 'Cancelled' AND DATE(appointmentdatetime) = CURDATE()) / " +
                    "(SELECT COUNT(*) FROM rendezvous WHERE DATE(appointmentdatetime) = CURDATE()))" +
                    ") AS no_show_rate, " +
                    "(SELECT COUNT(*) FROM patient WHERE DATE(created_at) = CURDATE()) AS registered_patients";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                int total = rs.getInt("total_appointments");
                double noShow = rs.getDouble("no_show_rate");
                int registered = rs.getInt("registered_patients");
                stats = new Statistics(total, noShow, registered);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stats;
    }

    private void fetchAppointments() {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Upcoming appointment: the very next appointment for today after the current time
            String upcomingSql = "SELECT p.fname, p.lname, a.appointmentdatetime " +
                    "FROM rendezvous a " +
                    "JOIN patient p ON a.patientid = p.id " +
                    "WHERE DATE(a.appointmentdatetime) = CURDATE() " +
                    "AND a.appointmentdatetime > NOW() " +
                    "ORDER BY a.appointmentdatetime ASC LIMIT 1";
            Statement upcomingStmt = conn.createStatement();
            ResultSet upcomingRs = upcomingStmt.executeQuery(upcomingSql);

            if (upcomingRs.next()) {
                String name = upcomingRs.getString("fname") + " " + upcomingRs.getString("lname");
                String datetime = upcomingRs.getString("appointmentdatetime");
                upComingClientName.setText(name);
                // Extract time portion safely (assuming format "yyyy-MM-dd HH:mm:ss")
                upComingTime.setText(datetime.substring(11, 16));
            } else {
                upComingClientName.setText("N/A");
                upComingTime.setText("N/A");
            }

            // Missed appointment: the most recent cancelled appointment for today that occurred before now
            String missedSql = "SELECT p.fname, p.lname, a.appointmentdatetime " +
                    "FROM rendezvous a " +
                    "JOIN patient p ON a.patientid = p.id " +
                    "WHERE a.status = 'Cancelled' " +
                    "AND DATE(a.appointmentdatetime) = CURDATE() " +
                    "AND a.appointmentdatetime < NOW() " +
                    "ORDER BY a.appointmentdatetime DESC LIMIT 1";
            Statement missedStmt = conn.createStatement();
            ResultSet missedRs = missedStmt.executeQuery(missedSql);

            if (missedRs.next()) {
                String name = missedRs.getString("fname") + " " + missedRs.getString("lname");
                String datetime = missedRs.getString("appointmentdatetime");
                missedClientName.setText(name);
                missedTime.setText(datetime.substring(11, 16));
            } else {
                missedClientName.setText("N/A");
                missedTime.setText("N/A");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateChart() {
        // Clear existing data and set a chart title
        chart.getData().clear();
        chart.setTitle("Appointments by Hour (Today)");

        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            String sql = "SELECT HOUR(appointmentdatetime) AS hour, COUNT(*) AS count " +
                    "FROM rendezvous " +
                    "WHERE DATE(appointmentdatetime) = CURDATE() " +
                    "GROUP BY hour " +
                    "ORDER BY hour";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            XYChart.Series<String, Double> series = new XYChart.Series<>();
            series.setName("Appointments");

            while (rs.next()) {
                int hour = rs.getInt("hour");
                int count = rs.getInt("count");
                series.getData().add(new XYChart.Data<>(String.format("%02d:00", hour), (double) count));
            }

            chart.getData().add(series);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupButtonVisibility() {
        String userRole = UserSession.getInstance().getRole();

        // Show or hide buttons based on user role
        if ("admin".equals(userRole)) {
            // Optionally enable admin-only buttons
            // gestionUtilisateur.setVisible(true);
            // statistiqueGlobales.setVisible(true);
        } else if ("medecin".equals(userRole) || "personnel".equals(userRole)) {
            gestionUtilisateur.setVisible(false);
            statistiqueGlobales.setVisible(false);
            sauvegarde.setVisible(false);
            rapportQuotidien.setVisible(false);
        }
    }

    public void onHome(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/Dashboard.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onCalendar(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/gestionRendezeVous.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onCreditCard(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/gestionPaiment.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onFolder(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/gestionDossierPatient.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onGlobalStats(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/statistiquesGlobales.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onRepport(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/rapportQuotidien.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onBackUp(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/sauvegarde.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onUsers(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/gestionUtilisateurs.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onLogOut(ActionEvent actionEvent) {
        try {
            SwitchScene.switchScene(actionEvent, "/com/example/myjavafxapp/loginForm.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
