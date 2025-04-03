package com.example.myjavafxapp.Models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PaymentManager {
    private static PaymentManager instance;

    // CRITICAL: These must match EXACTLY what's in the database enum - check carefully for spaces and case
    public static final String PAYMENT_METHOD_CASH = "Cash";
    public static final String PAYMENT_METHOD_CREDIT_CARD = "Credit Card";
    public static final String PAYMENT_METHOD_INSURANCE = "Insurance";

    // List of valid payment methods for validation and UI
    private static final List<String> VALID_PAYMENT_METHODS =
            Arrays.asList(PAYMENT_METHOD_CASH, PAYMENT_METHOD_CREDIT_CARD, PAYMENT_METHOD_INSURANCE);

    private PaymentManager() {}

    public static synchronized PaymentManager getInstance() {
        if (instance == null) {
            instance = new PaymentManager();
        }
        return instance;
    }

    /**
     * Get valid payment methods for UI components
     */
    public ObservableList<String> getValidPaymentMethods() {
        return FXCollections.observableArrayList(VALID_PAYMENT_METHODS);
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
            System.err.println("Error getting all payments: " + e.getMessage());
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
     * Verify that a payment method string exactly matches one of the valid enum values
     */
    public boolean isValidPaymentMethod(String method) {
        return VALID_PAYMENT_METHODS.contains(method);
    }

    /**
     * CRITICAL: Force a specific valid payment method to ensure it's always valid for the database
     * @param method A method that might not be valid
     * @return A guaranteed valid payment method from the ENUM
     */
    private String enforceValidPaymentMethod(String method) {
        // First, check direct equality with our known valid values
        if (PAYMENT_METHOD_CASH.equals(method)) {
            return PAYMENT_METHOD_CASH;
        }
        if (PAYMENT_METHOD_CREDIT_CARD.equals(method)) {
            return PAYMENT_METHOD_CREDIT_CARD;
        }
        if (PAYMENT_METHOD_INSURANCE.equals(method)) {
            return PAYMENT_METHOD_INSURANCE;
        }

        // If not a direct match, try to find the closest match
        String lowerMethod = method.toLowerCase().trim();

        if (lowerMethod.contains("cash") || lowerMethod.contains("espèces")) {
            return PAYMENT_METHOD_CASH;
        }
        if (lowerMethod.contains("credit") || lowerMethod.contains("carte")) {
            return PAYMENT_METHOD_CREDIT_CARD;
        }
        if (lowerMethod.contains("insurance") || lowerMethod.contains("assurance")) {
            return PAYMENT_METHOD_INSURANCE;
        }

        // Default to Cash if no match found
        System.out.println("Warning: Unrecognized payment method '" + method + "', defaulting to Cash");
        return PAYMENT_METHOD_CASH;
    }

    /**
     * Create a new payment
     */
    public boolean createPayment(Payment payment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // CRITICAL FIX: Use enforceValidPaymentMethod to guarantee a valid value
            String validatedMethod = enforceValidPaymentMethod(payment.getPaymentMethod());

            // Debug output to see what's being sent to the database
            System.out.println("Creating payment for appointment ID: " + payment.getRendezVousID());
            System.out.println("Original payment method: '" + payment.getPaymentMethod() + "'");
            System.out.println("Validated payment method: '" + validatedMethod + "'");
            System.out.println("Bytes: " + Arrays.toString(validatedMethod.getBytes()));

            String insertQuery = "INSERT INTO paiment (PatientID, RendezVousID, Amount, PaymentMethod, PaymentDate) " +
                    "VALUES (?, ?, ?, ?, NOW())";

            PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, payment.getPatientID());
            pstmt.setInt(2, payment.getRendezVousID());
            pstmt.setDouble(3, payment.getAmount());

            // Use the guaranteed valid method
            pstmt.setString(4, validatedMethod);

            int result = pstmt.executeUpdate();

            if (result > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    payment.setPaymentID(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("SQL error creating payment: " + e.getMessage());
            e.printStackTrace();

            // Let's try to get database column info to debug
            try {
                System.err.println("Checking PaymentMethod column in database:");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM paiment LIKE 'PaymentMethod'");
                if (rs.next()) {
                    String type = rs.getString("Type");
                    System.err.println("Column type: " + type);

                    // If it's an enum, extract the allowed values
                    if (type.startsWith("enum(")) {
                        String values = type.substring(5, type.length() - 1);
                        System.err.println("Allowed values: " + values);
                    }
                }
            } catch (SQLException e2) {
                System.err.println("Couldn't get column info: " + e2.getMessage());
            }
        }

        return false;
    }

    /**
     * Update an existing payment
     */
    public boolean updatePayment(Payment payment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // CRITICAL FIX: Use enforceValidPaymentMethod to guarantee a valid value
            String validatedMethod = enforceValidPaymentMethod(payment.getPaymentMethod());

            // Debug output
            System.out.println("Updating payment ID: " + payment.getPaymentID());
            System.out.println("Validated payment method: '" + validatedMethod + "'");

            String updateQuery = "UPDATE paiment SET Amount = ?, PaymentMethod = ? WHERE PaimentID = ?";

            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setDouble(1, payment.getAmount());
            pstmt.setString(2, validatedMethod);
            pstmt.setInt(3, payment.getPaymentID());

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("SQL error updating payment: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete a payment
     */
    public boolean deletePayment(int paymentID) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String deleteQuery = "DELETE FROM paiment WHERE PaimentID = ?";

            PreparedStatement pstmt = conn.prepareStatement(deleteQuery);
            pstmt.setInt(1, paymentID);

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
        try {
            // Use the correct column name: PaimentID instead of PaymentID
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
        } catch (SQLException e) {
            System.err.println("Error in createPaymentFromResultSet: " + e.getMessage());
            // Add debugging information
            System.err.println("Available columns in the result set:");
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                System.err.println(i + ": " + metaData.getColumnName(i) + " (" + metaData.getColumnTypeName(i) + ")");
            }
            throw e;
        }
        return payment;
    }
}