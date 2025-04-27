package com.example.myjavafxapp.Models.reports;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class for generating daily reports
 */
public class DailyReportManager {
    private static DailyReportManager instance;

    private DailyReportManager() {}

    public static synchronized DailyReportManager getInstance() {
        if (instance == null) {
            instance = new DailyReportManager();
        }
        return instance;
    }

    /**
     * Generate a daily report for the specified date
     */
    public DailyReport generateDailyReport(LocalDate date) {
        DailyReport report = new DailyReport(date);
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Check if the report is partial (if date is today and time is before 17:30)
            LocalDateTime now = LocalDateTime.now();
            if (date.equals(LocalDate.now()) && now.toLocalTime().isBefore(LocalTime.of(17, 30))) {
                report.setPartial(true);
            }

            // Get total appointments for the day
            String totalQuery = "SELECT COUNT(*) FROM rendezvous WHERE DATE(AppointmentDateTime) = ?";
            PreparedStatement totalStmt = conn.prepareStatement(totalQuery);
            totalStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet totalRs = totalStmt.executeQuery();

            if (totalRs.next()) {
                report.setTotalAppointments(totalRs.getInt(1));
            }

            // Get completed appointments
            String completedQuery = "SELECT COUNT(*) FROM rendezvous WHERE DATE(AppointmentDateTime) = ? AND Status = 'Completed'";
            PreparedStatement completedStmt = conn.prepareStatement(completedQuery);
            completedStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet completedRs = completedStmt.executeQuery();

            if (completedRs.next()) {
                report.setCompletedAppointments(completedRs.getInt(1));
            }

            // Get canceled appointments
            String canceledQuery = "SELECT COUNT(*) FROM rendezvous WHERE DATE(AppointmentDateTime) = ? " +
                    "AND (Status = 'Patient_Cancelled' OR Status = 'Clinic_Cancelled')";
            PreparedStatement canceledStmt = conn.prepareStatement(canceledQuery);
            canceledStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet canceledRs = canceledStmt.executeQuery();

            if (canceledRs.next()) {
                report.setCanceledAppointments(canceledRs.getInt(1));
            }

            // Get patients seen with detailed information including payment method
            String patientsQuery = "SELECT p.ID, p.FNAME, p.LNAME, r.AppointmentDateTime, pm.PaymentMethod " +
                    "FROM patient p " +
                    "JOIN rendezvous r ON p.ID = r.PatientID " +
                    "LEFT JOIN paiment pm ON r.RendezVousID = pm.RendezVousID " +
                    "WHERE DATE(r.AppointmentDateTime) = ? AND r.Status = 'Completed' " +
                    "ORDER BY r.AppointmentDateTime";
            PreparedStatement patientsStmt = conn.prepareStatement(patientsQuery);
            patientsStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet patientsRs = patientsStmt.executeQuery();

            List<DailyReport.PatientVisit> patientVisits = new ArrayList<>();
            while (patientsRs.next()) {
                String cin = patientsRs.getString("ID");
                String firstName = patientsRs.getString("FNAME");
                String lastName = patientsRs.getString("LNAME");
                Timestamp timestamp = patientsRs.getTimestamp("AppointmentDateTime");
                LocalDateTime visitTime = timestamp.toLocalDateTime();
                String paymentMethod = patientsRs.getString("PaymentMethod");

                DailyReport.PatientVisit visit = new DailyReport.PatientVisit(cin, firstName, lastName, visitTime, paymentMethod);
                patientVisits.add(visit);
            }
            report.setPatientsVisits(patientVisits);

            // Get appointments by hour
            String hourlyQuery = "SELECT HOUR(AppointmentDateTime) as hour, COUNT(*) as count " +
                    "FROM rendezvous " +
                    "WHERE DATE(AppointmentDateTime) = ? " +
                    "GROUP BY HOUR(AppointmentDateTime) " +
                    "ORDER BY hour";
            PreparedStatement hourlyStmt = conn.prepareStatement(hourlyQuery);
            hourlyStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet hourlyRs = hourlyStmt.executeQuery();

            Map<LocalTime, Integer> appointmentsByHour = new HashMap<>();
            while (hourlyRs.next()) {
                int hour = hourlyRs.getInt("hour");
                int count = hourlyRs.getInt("count");
                appointmentsByHour.put(LocalTime.of(hour, 0), count);
            }
            report.setAppointmentsByHour(appointmentsByHour);

            // Get total revenue
            String revenueQuery = "SELECT COALESCE(SUM(p.Amount), 0) as totalRevenue " +
                    "FROM paiment p " +
                    "JOIN rendezvous r ON p.RendezVousID = r.RendezVousID " +
                    "WHERE DATE(r.AppointmentDateTime) = ?";
            PreparedStatement revenueStmt = conn.prepareStatement(revenueQuery);
            revenueStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet revenueRs = revenueStmt.executeQuery();

            if (revenueRs.next()) {
                report.setTotalRevenue(revenueRs.getDouble("totalRevenue"));
            }

            // Get revenue by payment method
            String paymentMethodQuery = "SELECT p.PaymentMethod, COALESCE(SUM(p.Amount), 0) as amount " +
                    "FROM paiment p " +
                    "JOIN rendezvous r ON p.RendezVousID = r.RendezVousID " +
                    "WHERE DATE(r.AppointmentDateTime) = ? " +
                    "GROUP BY p.PaymentMethod";
            PreparedStatement paymentMethodStmt = conn.prepareStatement(paymentMethodQuery);
            paymentMethodStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet paymentMethodRs = paymentMethodStmt.executeQuery();

            Map<String, Double> revenueByPaymentMethod = new HashMap<>();
            while (paymentMethodRs.next()) {
                String method = paymentMethodRs.getString("PaymentMethod");
                double amount = paymentMethodRs.getDouble("amount");
                revenueByPaymentMethod.put(method, amount);
            }
            report.setRevenueByPaymentMethod(revenueByPaymentMethod);

            // Get patients by doctor
            String doctorQuery = "SELECT u.FNAME, u.LNAME, COUNT(DISTINCT r.PatientID) as patientCount " +
                    "FROM users u " +
                    "JOIN rendezvous r ON u.ID = r.MedecinID " +
                    "WHERE DATE(r.AppointmentDateTime) = ? " +
                    "GROUP BY u.ID";
            PreparedStatement doctorStmt = conn.prepareStatement(doctorQuery);
            doctorStmt.setDate(1, java.sql.Date.valueOf(date));
            ResultSet doctorRs = doctorStmt.executeQuery();

            Map<String, Integer> patientsByDoctor = new HashMap<>();
            while (doctorRs.next()) {
                String doctorName = "Dr. " + doctorRs.getString("LNAME") + ", " + doctorRs.getString("FNAME");
                int patientCount = doctorRs.getInt("patientCount");
                patientsByDoctor.put(doctorName, patientCount);
            }
            report.setPatientsByDoctor(patientsByDoctor);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return report;
    }
}