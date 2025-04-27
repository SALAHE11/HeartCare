package com.example.myjavafxapp.Models.reports;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager class for statistics-related operations
 */
public class StatisticsManager {
    private static StatisticsManager instance;

    private StatisticsManager() {}

    public static synchronized StatisticsManager getInstance() {
        if (instance == null) {
            instance = new StatisticsManager();
        }
        return instance;
    }

    /**
     * Get patient demographics for a given date range
     */
    public Map<String, Object> getPatientDemographics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> demographics = new HashMap<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Total patients
            String totalQuery = "SELECT COUNT(*) FROM patient";
            PreparedStatement totalStmt = conn.prepareStatement(totalQuery);
            ResultSet totalRs = totalStmt.executeQuery();

            if (totalRs.next()) {
                demographics.put("totalPatients", totalRs.getInt(1));
            }

            // New patients in date range
            String newQuery = "SELECT COUNT(*) FROM patient WHERE CREATED_AT BETWEEN ? AND ?";
            PreparedStatement newStmt = conn.prepareStatement(newQuery);
            newStmt.setDate(1, java.sql.Date.valueOf(startDate));
            newStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet newRs = newStmt.executeQuery();

            if (newRs.next()) {
                demographics.put("newPatients", newRs.getInt(1));
            }

            // Gender distribution
            String genderQuery = "SELECT SEXE, COUNT(*) FROM patient GROUP BY SEXE";
            PreparedStatement genderStmt = conn.prepareStatement(genderQuery);
            ResultSet genderRs = genderStmt.executeQuery();

            Map<String, Integer> genderCounts = new HashMap<>();
            while (genderRs.next()) {
                String gender = genderRs.getString(1);
                int count = genderRs.getInt(2);
                genderCounts.put(gender, count);
            }
            demographics.put("genderDistribution", genderCounts);

            // Age distribution
            // Note: This is a simplified approach. In a real application, you would
            // calculate age based on birthdate and current date.
            String ageQuery =
                    "SELECT " +
                            "  CASE " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 10 THEN '0-10' " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 20 THEN '11-20' " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 30 THEN '21-30' " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 40 THEN '31-40' " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 50 THEN '41-50' " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 60 THEN '51-60' " +
                            "    WHEN TIMESTAMPDIFF(YEAR, BIRTHDATE, CURDATE()) <= 70 THEN '61-70' " +
                            "    ELSE '71+' " +
                            "  END AS age_group, " +
                            "  COUNT(*) as count " +
                            "FROM patient " +
                            "GROUP BY age_group " +
                            "ORDER BY FIELD(age_group, '0-10', '11-20', '21-30', '31-40', '41-50', '51-60', '61-70', '71+')";

            PreparedStatement ageStmt = conn.prepareStatement(ageQuery);
            ResultSet ageRs = ageStmt.executeQuery();

            Map<String, Integer> ageGroups = new HashMap<>();
            while (ageRs.next()) {
                String ageGroup = ageRs.getString("age_group");
                int count = ageRs.getInt("count");
                ageGroups.put(ageGroup, count);
            }
            demographics.put("ageDistribution", ageGroups);

            // Patients per doctor (appointments in date range)
            String perDoctorQuery =
                    "SELECT u.ID, u.FNAME, u.LNAME, COUNT(DISTINCT r.PatientID) as patientCount " +
                            "FROM users u " +
                            "JOIN rendezvous r ON u.ID = r.MedecinID " +
                            "WHERE u.ROLE = 'medecin' " +
                            "AND r.AppointmentDateTime BETWEEN ? AND ? " +
                            "GROUP BY u.ID " +
                            "ORDER BY patientCount DESC";

            PreparedStatement perDoctorStmt = conn.prepareStatement(perDoctorQuery);
            perDoctorStmt.setDate(1, java.sql.Date.valueOf(startDate));
            perDoctorStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet perDoctorRs = perDoctorStmt.executeQuery();

            List<Map<String, Object>> doctorPatients = new ArrayList<>();
            while (perDoctorRs.next()) {
                Map<String, Object> doctorData = new HashMap<>();
                doctorData.put("id", perDoctorRs.getString("ID"));
                doctorData.put("name", perDoctorRs.getString("LNAME") + ", " + perDoctorRs.getString("FNAME"));
                doctorData.put("patientCount", perDoctorRs.getInt("patientCount"));
                doctorPatients.add(doctorData);
            }
            demographics.put("patientsPerDoctor", doctorPatients);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return demographics;
    }

    /**
     * Get financial statistics for a given date range
     */
    public Map<String, Object> getFinancialStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Get completed appointments count
            String completedQuery =
                    "SELECT COUNT(*) " +
                            "FROM rendezvous " +
                            "WHERE Status = 'Completed' " +
                            "AND DATE(AppointmentDateTime) BETWEEN ? AND ?";

            PreparedStatement completedStmt = conn.prepareStatement(completedQuery);
            completedStmt.setDate(1, java.sql.Date.valueOf(startDate));
            completedStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet completedRs = completedStmt.executeQuery();

            int completedCount = 0;
            if (completedRs.next()) {
                completedCount = completedRs.getInt(1);
            }
            statistics.put("completedAppointments", completedCount);

            // Calculate total revenue from actual payments
            String revenueQuery =
                    "SELECT COALESCE(SUM(p.Amount), 0) as totalRevenue " +
                            "FROM paiment p " +
                            "JOIN rendezvous r ON p.RendezVousID = r.RendezVousID " +
                            "WHERE r.Status = 'Completed' " +
                            "AND DATE(r.AppointmentDateTime) BETWEEN ? AND ?";

            PreparedStatement revenueStmt = conn.prepareStatement(revenueQuery);
            revenueStmt.setDate(1, java.sql.Date.valueOf(startDate));
            revenueStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet revenueRs = revenueStmt.executeQuery();

            double totalRevenue = 0.0;
            if (revenueRs.next()) {
                totalRevenue = revenueRs.getDouble("totalRevenue");
            }
            statistics.put("totalRevenue", totalRevenue);

            // Count unique patients in completed appointments
            String uniquePatientsQuery =
                    "SELECT COUNT(DISTINCT PatientID) " +
                            "FROM rendezvous " +
                            "WHERE Status = 'Completed' " +
                            "AND DATE(AppointmentDateTime) BETWEEN ? AND ?";

            PreparedStatement uniquePatientsStmt = conn.prepareStatement(uniquePatientsQuery);
            uniquePatientsStmt.setDate(1, java.sql.Date.valueOf(startDate));
            uniquePatientsStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet uniquePatientsRs = uniquePatientsStmt.executeQuery();

            int uniquePatients = 0;
            if (uniquePatientsRs.next()) {
                uniquePatients = uniquePatientsRs.getInt(1);
            }
            statistics.put("uniquePatients", uniquePatients);

            // Calculate averages
            double avgRevenuePerAppointment = completedCount > 0 ? totalRevenue / completedCount : 0;
            double avgRevenuePerPatient = uniquePatients > 0 ? totalRevenue / uniquePatients : 0;

            statistics.put("avgRevenuePerAppointment", avgRevenuePerAppointment);
            statistics.put("avgRevenuePerPatient", avgRevenuePerPatient);

            // Calculate revenue by doctor
            String revenueByDoctorQuery =
                    "SELECT u.ID, u.FNAME, u.LNAME, " +
                            "COUNT(r.RendezVousID) as appointmentCount, " +
                            "COALESCE(SUM(p.Amount), 0) as revenue " +
                            "FROM users u " +
                            "JOIN rendezvous r ON u.ID = r.MedecinID " +
                            "LEFT JOIN paiment p ON r.RendezVousID = p.RendezVousID " +
                            "WHERE u.ROLE = 'medecin' " +
                            "AND r.Status = 'Completed' " +
                            "AND DATE(r.AppointmentDateTime) BETWEEN ? AND ? " +
                            "GROUP BY u.ID " +
                            "ORDER BY revenue DESC";

            PreparedStatement revenueByDoctorStmt = conn.prepareStatement(revenueByDoctorQuery);
            revenueByDoctorStmt.setDate(1, java.sql.Date.valueOf(startDate));
            revenueByDoctorStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet revenueByDoctorRs = revenueByDoctorStmt.executeQuery();

            List<Map<String, Object>> doctorRevenue = new ArrayList<>();
            while (revenueByDoctorRs.next()) {
                Map<String, Object> doctorData = new HashMap<>();
                doctorData.put("id", revenueByDoctorRs.getString("ID"));
                doctorData.put("name", revenueByDoctorRs.getString("LNAME") + ", " + revenueByDoctorRs.getString("FNAME"));
                doctorData.put("appointmentCount", revenueByDoctorRs.getInt("appointmentCount"));
                doctorData.put("revenue", revenueByDoctorRs.getDouble("revenue"));
                doctorRevenue.add(doctorData);
            }
            statistics.put("revenueByDoctor", doctorRevenue);

            // Calculate revenue by period (month, week or day depending on date range)
            int daysBetween = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

            String periodGroup;
            String periodLabel;

            if (daysBetween <= 7) {
                // Daily data
                periodGroup = "DATE(r.AppointmentDateTime)";
                periodLabel = "day";
            } else if (daysBetween <= 90) {
                // Weekly data
                periodGroup = "YEARWEEK(r.AppointmentDateTime, 3)"; // ISO week
                periodLabel = "week";
            } else {
                // Monthly data
                periodGroup = "DATE_FORMAT(r.AppointmentDateTime, '%Y-%m')";
                periodLabel = "month";
            }

            String revenueByPeriodQuery =
                    "SELECT " + periodGroup + " as period, " +
                            "COALESCE(SUM(p.Amount), 0) as revenue " +
                            "FROM rendezvous r " +
                            "LEFT JOIN paiment p ON r.RendezVousID = p.RendezVousID " +
                            "WHERE r.Status = 'Completed' " +
                            "AND DATE(r.AppointmentDateTime) BETWEEN ? AND ? " +
                            "GROUP BY period " +
                            "ORDER BY period";

            PreparedStatement revenueByPeriodStmt = conn.prepareStatement(revenueByPeriodQuery);
            revenueByPeriodStmt.setDate(1, java.sql.Date.valueOf(startDate));
            revenueByPeriodStmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet revenueByPeriodRs = revenueByPeriodStmt.executeQuery();

            Map<String, Double> periodRevenue = new HashMap<>();
            while (revenueByPeriodRs.next()) {
                String period = revenueByPeriodRs.getString("period");
                double revenue = revenueByPeriodRs.getDouble("revenue");
                periodRevenue.put(period, revenue);
            }

            statistics.put("revenueByPeriod", periodRevenue);
            statistics.put("periodType", periodLabel);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return statistics;
    }

    /**
     * Get appointment statistics by day of week
     */
    public Map<String, Integer> getAppointmentsByDayOfWeek(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> dayOfWeekCounts = new HashMap<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query =
                    "SELECT DAYNAME(AppointmentDateTime) as day_name, COUNT(*) as count " +
                            "FROM rendezvous " +
                            "WHERE DATE(AppointmentDateTime) BETWEEN ? AND ? " +
                            "GROUP BY day_name " +
                            "ORDER BY FIELD(day_name, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String dayName = rs.getString("day_name");
                int count = rs.getInt("count");
                dayOfWeekCounts.put(dayName, count);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dayOfWeekCounts;
    }

    /**
     * Get appointment statistics by hour of day
     */
    public Map<String, Integer> getAppointmentsByHour(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> hourCounts = new HashMap<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query =
                    "SELECT " +
                            "  CONCAT(HOUR(AppointmentDateTime), ':', " +
                            "         IF(MINUTE(AppointmentDateTime) < 30, '00', '30')) as hour_slot, " +
                            "  COUNT(*) as count " +
                            "FROM rendezvous " +
                            "WHERE DATE(AppointmentDateTime) BETWEEN ? AND ? " +
                            "GROUP BY hour_slot " +
                            "ORDER BY HOUR(AppointmentDateTime), MINUTE(AppointmentDateTime)";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String hourSlot = rs.getString("hour_slot");
                int count = rs.getInt("count");
                hourCounts.put(hourSlot, count);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return hourCounts;
    }
}