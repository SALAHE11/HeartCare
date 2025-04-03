package com.example.myjavafxapp.Models;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentManager {
    private static PaymentManager instance;

    private PaymentManager() {}

    public static synchronized PaymentManager getInstance() {
        if (instance == null) {
            instance = new PaymentManager();
        }
        return instance;
    }

    /**
     * Get all payments
     */
    public List<Payment> getAllPayments() {
        List<Payment> payments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT p.*, r.AppointmentDateTime, " +
                    "CONCAT(pt.FNAME, ' ', pt.LNAME) as PatientName " +
                    "FROM paiment p " +
                    "JOIN rendezvous r ON p.RendezVousID = r.RendezVousID " +
                    "JOIN patient pt ON p.PatientID = pt.ID " +
                    "ORDER BY p.PaymentDate DESC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Payment payment = createPaymentFromResultSet(rs);
                payments.add(payment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return payments;
    }

    /**
     * Search for completed appointments by patient CIN for the current day
     */
    public List<Appointment> searchCompletedAppointmentsByPatientCIN(String patientCIN) {
        List<Appointment> completedAppointments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        LocalDate today = LocalDate.now();

        try {
            String query = "SELECT r.RendezVousID, r.PatientID, r.MedecinID, " +
                    "r.AppointmentDateTime, r.ReasonForVisit, r.Status, r.StatusReason, " +
                    "r.NoShowFlag, r.RescheduledToID, r.CancellationTime, r.Priority, " +
                    "CONCAT(p.FNAME, ' ', p.LNAME) as PatientName, " +
                    "CONCAT(u.FNAME, ' ', u.LNAME) as DoctorName " +
                    "FROM rendezvous r " +
                    "JOIN patient p ON r.PatientID = p.ID " +
                    "JOIN users u ON r.MedecinID = u.ID " +
                    "WHERE r.PatientID = ? " +
                    "AND r.Status = 'Completed' " +
                    "AND DATE(r.AppointmentDateTime) = ? " +
                    "ORDER BY r.AppointmentDateTime DESC";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, patientCIN);
            pstmt.setDate(2, java.sql.Date.valueOf(today));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Appointment appointment = new Appointment();
                appointment.setRendezVousID(rs.getInt("RendezVousID"));
                appointment.setPatientID(rs.getString("PatientID"));
                appointment.setMedicinID(rs.getString("MedecinID"));
                appointment.setAppointmentDateTime(rs.getTimestamp("AppointmentDateTime").toLocalDateTime());
                appointment.setReasonForVisit(rs.getString("ReasonForVisit"));
                appointment.setStatus(rs.getString("Status"));
                appointment.setStatusReason(rs.getString("StatusReason"));
                appointment.setNoShowFlag(rs.getBoolean("NoShowFlag"));

                // Handle nullable fields
                int rescheduledToID = rs.getInt("RescheduledToID");
                if (!rs.wasNull()) {
                    appointment.setRescheduledToID(rescheduledToID);
                }

                Timestamp cancellationTime = rs.getTimestamp("CancellationTime");
                if (cancellationTime != null) {
                    appointment.setCancellationTime(cancellationTime.toLocalDateTime());
                }

                appointment.setPriority(rs.getString("Priority"));
                appointment.setPatientName(rs.getString("PatientName"));
                appointment.setDoctorName(rs.getString("DoctorName"));

                completedAppointments.add(appointment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return completedAppointments;
    }

    /**
     * Check if payment exists for an appointment
     */
    public boolean isAppointmentPaid(int rendezVousID) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
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
     * Create a new payment
     */
    public boolean createPayment(Payment payment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String insertQuery = "INSERT INTO paiment (PatientID, RendezVousID, Amount, PaymentMethod, PaymentDate) " +
                    "VALUES (?, ?, ?, ?, NOW())";

            PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, payment.getPatientID());
            pstmt.setInt(2, payment.getRendezVousID());
            pstmt.setDouble(3, payment.getAmount());
            pstmt.setString(4, payment.getPaymentMethod());

            int result = pstmt.executeUpdate();

            if (result > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    payment.setPaymentID(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing payment
     */
    public boolean updatePayment(Payment payment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String updateQuery = "UPDATE paiment SET Amount = ?, PaymentMethod = ? WHERE PaimentID = ?";

            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, payment.getAmount());
            pstmt.setString(2, payment.getPaymentMethod());
            pstmt.setInt(3, payment.getPaymentID());

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Helper method to create a Payment object from ResultSet
     */
    private Payment createPaymentFromResultSet(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentID(rs.getInt("PaimentID"));
        payment.setPatientID(rs.getString("PatientID"));
        payment.setRendezVousID(rs.getInt("RendezVousID"));
        payment.setAmount(rs.getDouble("Amount"));
        payment.setPaymentMethod(rs.getString("PaymentMethod"));
        payment.setPaymentDate(rs.getTimestamp("PaymentDate"));

        // Get additional information if available
        try {
            payment.setPatientName(rs.getString("PatientName"));
            Timestamp appointmentDateTime = rs.getTimestamp("AppointmentDateTime");
            if (appointmentDateTime != null) {
                payment.setAppointmentDateTime(appointmentDateTime.toLocalDateTime());
            }
        } catch (SQLException e) {
            // These fields might not be in the result set, ignore
        }

        return payment;
    }
}