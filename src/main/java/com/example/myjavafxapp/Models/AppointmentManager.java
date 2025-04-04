package com.example.myjavafxapp.Models;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentManager {
    private static AppointmentManager instance;

    private AppointmentManager() {}

    public static synchronized AppointmentManager getInstance() {
        if (instance == null) {
            instance = new AppointmentManager();
        }
        return instance;
    }

    /**
     * Get appointments for a specific date
     */
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        List<Appointment> appointments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT r.*, " +
                    "p.FNAME AS patientFName, p.LNAME AS patientLName, " +
                    "u.FNAME AS doctorFName, u.LNAME AS doctorLName " +
                    "FROM rendezvous r " +
                    "LEFT JOIN patient p ON r.PatientID = p.ID " +
                    "LEFT JOIN users u ON r.MedecinID = u.ID " +
                    "WHERE DATE(r.AppointmentDateTime) = ? " +
                    "ORDER BY r.AppointmentDateTime";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(date));

            System.out.println("Fetching appointments for date: " + date);
            ResultSet rs = pstmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                Appointment appointment = createAppointmentFromResultSet(rs);
                appointments.add(appointment);
                count++;
            }
            System.out.println("Found " + count + " appointments for date: " + date);

        } catch (SQLException e) {
            System.err.println("Error fetching appointments by date: " + e.getMessage());
            e.printStackTrace();
        }

        return appointments;
    }

    /**
     * Get appointments for a specific doctor
     */
    public List<Appointment> getAppointmentsByDoctor(String doctorID) {
        List<Appointment> appointments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT r.*, " +
                    "p.FNAME AS patientFName, p.LNAME AS patientLName, " +
                    "u.FNAME AS doctorFName, u.LNAME AS doctorLName " +
                    "FROM rendezvous r " +
                    "LEFT JOIN patient p ON r.PatientID = p.ID " +
                    "LEFT JOIN users u ON r.MedecinID = u.ID " +
                    "WHERE r.MedecinID = ? " +
                    "ORDER BY r.AppointmentDateTime";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, doctorID);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Appointment appointment = createAppointmentFromResultSet(rs);
                appointments.add(appointment);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return appointments;
    }

    /**
     * Get appointments for a specific patient
     */
    public List<Appointment> getAppointmentsByPatient(String patientID) {
        List<Appointment> appointments = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT r.*, " +
                    "p.FNAME AS patientFName, p.LNAME AS patientLName, " +
                    "u.FNAME AS doctorFName, u.LNAME AS doctorLName " +
                    "FROM rendezvous r " +
                    "LEFT JOIN patient p ON r.PatientID = p.ID " +
                    "LEFT JOIN users u ON r.MedecinID = u.ID " +
                    "WHERE r.PatientID = ? " +
                    "ORDER BY r.AppointmentDateTime DESC";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, patientID);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Appointment appointment = createAppointmentFromResultSet(rs);
                appointments.add(appointment);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return appointments;
    }

    /**
     * Get a single appointment by ID
     */
    public Appointment getAppointmentById(int appointmentId) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT r.*, " +
                    "p.FNAME AS patientFName, p.LNAME AS patientLName, " +
                    "u.FNAME AS doctorFName, u.LNAME AS doctorLName " +
                    "FROM rendezvous r " +
                    "LEFT JOIN patient p ON r.PatientID = p.ID " +
                    "LEFT JOIN users u ON r.MedecinID = u.ID " +
                    "WHERE r.RendezVousID = ?";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, appointmentId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return createAppointmentFromResultSet(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Create a new appointment
     */
    public boolean createAppointment(Appointment appointment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Make sure none of the required fields are null
            if (appointment.getPatientID() == null || appointment.getMedicinID() == null ||
                    appointment.getAppointmentDateTime() == null || appointment.getStatus() == null) {
                System.err.println("Cannot create appointment with null required fields");
                return false;
            }

            // Ensure other fields have default values if null
            if (appointment.getStatusReason() == null) appointment.setStatusReason("");
            if (appointment.getReasonForVisit() == null) appointment.setReasonForVisit("");
            if (appointment.getPriority() == null) appointment.setPriority("Normal");

            String insertQuery = "INSERT INTO rendezvous " +
                    "(PatientID, MedecinID, AppointmentDateTime, ReasonForVisit, " +
                    "Status, StatusReason, NoShowFlag, Priority, DateCreated, LastUpdated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

            PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, appointment.getPatientID());
            pstmt.setString(2, appointment.getMedicinID());
            pstmt.setTimestamp(3, Timestamp.valueOf(appointment.getAppointmentDateTime()));
            pstmt.setString(4, appointment.getReasonForVisit());
            pstmt.setString(5, appointment.getStatus());
            pstmt.setString(6, appointment.getStatusReason());
            pstmt.setBoolean(7, appointment.isNoShowFlag());
            pstmt.setString(8, appointment.getPriority());

            System.out.println("Executing query: " + pstmt.toString());
            int result = pstmt.executeUpdate();

            if (result > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    appointment.setRendezVousID(newId);

                    // Log the creation in history
                    logStatusChange(newId, null, "Scheduled",
                            appointment.getStatusReason(),
                            UserSession.getInstance().getUsername());

                    System.out.println("Successfully created appointment with ID: " + newId);
                }
                return true;
            } else {
                System.err.println("No rows affected when creating appointment");
            }

        } catch (SQLException e) {
            System.err.println("SQL Exception when creating appointment: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error when creating appointment: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing appointment
     */
    public boolean updateAppointment(Appointment appointment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Get current appointment to check for status change
            Appointment currentAppointment = getAppointmentById(appointment.getRendezVousID());

            String updateQuery = "UPDATE rendezvous SET " +
                    "PatientID = ?, MedecinID = ?, AppointmentDateTime = ?, " +
                    "ReasonForVisit = ?, Status = ?, StatusReason = ?, " +
                    "NoShowFlag = ?, RescheduledToID = ?, " +
                    "CancellationTime = ?, Priority = ?, LastUpdated = NOW() " +
                    "WHERE RendezVousID = ?";

            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setString(1, appointment.getPatientID());
            pstmt.setString(2, appointment.getMedicinID());
            pstmt.setTimestamp(3, Timestamp.valueOf(appointment.getAppointmentDateTime()));
            pstmt.setString(4, appointment.getReasonForVisit());
            pstmt.setString(5, appointment.getStatus());
            pstmt.setString(6, appointment.getStatusReason());
            pstmt.setBoolean(7, appointment.isNoShowFlag());

            if (appointment.getRescheduledToID() != null) {
                pstmt.setInt(8, appointment.getRescheduledToID());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }

            if (appointment.getCancellationTime() != null) {
                pstmt.setTimestamp(9, Timestamp.valueOf(appointment.getCancellationTime()));
            } else {
                pstmt.setNull(9, java.sql.Types.TIMESTAMP);
            }

            pstmt.setString(10, appointment.getPriority());
            pstmt.setInt(11, appointment.getRendezVousID());

            int result = pstmt.executeUpdate();

            // Log status change if status has changed
            if (result > 0 && currentAppointment != null &&
                    !currentAppointment.getStatus().equals(appointment.getStatus())) {
                logStatusChange(
                        appointment.getRendezVousID(),
                        currentAppointment.getStatus(),
                        appointment.getStatus(),
                        appointment.getStatusReason(),
                        UserSession.getInstance().getUsername()
                );
            }

            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update appointment status
     */
    public boolean updateAppointmentStatus(int appointmentId, String newStatus, String reason) {
        Appointment appointment = getAppointmentById(appointmentId);
        if (appointment == null) {
            return false;
        }

        String oldStatus = appointment.getStatus();
        appointment.setStatus(newStatus);
        appointment.setStatusReason(reason);

        // Set additional fields based on status
        if ("Missed".equals(newStatus)) {
            appointment.setNoShowFlag(true);
        } else if ("Rescheduled".equals(newStatus)) {
            // Rescheduling happens elsewhere
        } else if ("Patient_Cancelled".equals(newStatus) || "Clinic_Cancelled".equals(newStatus)) {
            appointment.setCancellationTime(LocalDateTime.now());
        }

        boolean result = updateAppointment(appointment);

        // If status change is successful, log it
        if (result) {
            logStatusChange(
                    appointmentId,
                    oldStatus,
                    newStatus,
                    reason,
                    UserSession.getInstance().getUsername()
            );
        }

        return result;
    }

    /**
     * Check in a patient for their appointment
     */
    public boolean checkInAppointment(int appointmentId) {
        return updateAppointmentStatus(appointmentId, "CheckedIn", "Patient arrived");
    }

    /**
     * Start an appointment (move to in progress)
     */
    public boolean startAppointment(int appointmentId) {
        return updateAppointmentStatus(appointmentId, "InProgress", "Patient with doctor");
    }

    /**
     * Complete an appointment
     */
    public boolean completeAppointment(int appointmentId, String notes) {
        boolean result = updateAppointmentStatus(appointmentId, "Completed", notes);
        return result;
    }

    /**
     * Mark an appointment as missed
     */
    public boolean markAsMissed(int appointmentId, String reason) {
        Appointment appointment = getAppointmentById(appointmentId);
        if (appointment == null) {
            return false;
        }

        appointment.setStatus("Missed");
        appointment.setStatusReason(reason);
        appointment.setNoShowFlag(true);

        boolean result = updateAppointment(appointment);

        return result;
    }

    /**
     * Reschedule an appointment
     */
    public int rescheduleAppointment(int oldAppointmentId, LocalDateTime newDateTime, String reason) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            conn.setAutoCommit(false);

            // Get the original appointment
            Appointment oldAppointment = getAppointmentById(oldAppointmentId);
            if (oldAppointment == null) {
                conn.rollback();
                return -1;
            }

            // Create a new appointment record
            Appointment newAppointment = new Appointment();
            newAppointment.setPatientID(oldAppointment.getPatientID());
            newAppointment.setMedicinID(oldAppointment.getMedicinID());
            newAppointment.setAppointmentDateTime(newDateTime);
            newAppointment.setReasonForVisit(oldAppointment.getReasonForVisit());
            newAppointment.setStatus("Scheduled");
            newAppointment.setPriority(oldAppointment.getPriority());

            // Insert the new appointment
            String insertQuery = "INSERT INTO rendezvous (PatientID, MedecinID, AppointmentDateTime, " +
                    "ReasonForVisit, Status, Priority, DateCreated, LastUpdated) " +
                    "VALUES (?, ?, ?, ?, 'Scheduled', ?, NOW(), NOW())";

            PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, newAppointment.getPatientID());
            pstmt.setString(2, newAppointment.getMedicinID());
            pstmt.setTimestamp(3, Timestamp.valueOf(newAppointment.getAppointmentDateTime()));
            pstmt.setString(4, newAppointment.getReasonForVisit());
            pstmt.setString(5, newAppointment.getPriority());

            int result = pstmt.executeUpdate();

            if (result <= 0) {
                conn.rollback();
                return -1;
            }

            ResultSet rs = pstmt.getGeneratedKeys();
            int newAppointmentId = -1;
            if (rs.next()) {
                newAppointmentId = rs.getInt(1);
            } else {
                conn.rollback();
                return -1;
            }

            // Update the old appointment to Rescheduled status
            String updateQuery = "UPDATE rendezvous SET " +
                    "Status = 'Rescheduled', StatusReason = ?, " +
                    "RescheduledToID = ?, LastUpdated = NOW() " +
                    "WHERE RendezVousID = ?";

            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setString(1, reason);
            updateStmt.setInt(2, newAppointmentId);
            updateStmt.setInt(3, oldAppointmentId);

            result = updateStmt.executeUpdate();

            if (result <= 0) {
                conn.rollback();
                return -1;
            }

            // Log the status change
            logStatusChange(
                    oldAppointmentId,
                    oldAppointment.getStatus(),
                    "Rescheduled",
                    reason,
                    UserSession.getInstance().getUsername()
            );

            conn.commit();
            return newAppointmentId;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return -1;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Cancel an appointment
     */
    public boolean cancelAppointment(int appointmentId, String reason, boolean cancelledByPatient) {
        Appointment appointment = getAppointmentById(appointmentId);
        if (appointment == null) {
            return false;
        }

        String newStatus = cancelledByPatient ? "Patient_Cancelled" : "Clinic_Cancelled";

        appointment.setStatus(newStatus);
        appointment.setStatusReason(reason);
        appointment.setCancellationTime(LocalDateTime.now());

        return updateAppointment(appointment);
    }

    /**
     * Get all doctors from the users table with role 'medecin'
     */
    public List<User> getAllDoctors() {
        List<User> doctors = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM users WHERE ROLE = 'medecin'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                User doctor = new User();
                doctor.setId(rs.getString("ID"));
                doctor.setUsername(rs.getString("USERNAME"));
                doctor.setFirstName(rs.getString("FNAME"));
                doctor.setLastName(rs.getString("LNAME"));
                doctor.setRole(rs.getString("ROLE"));

                doctors.add(doctor);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return doctors;
    }

    /**
     * Get appointment statistics for a time period
     */
    public Map<String, Object> getAppointmentStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT " +
                    "COUNT(*) AS total, " +
                    "SUM(CASE WHEN Status = 'Completed' THEN 1 ELSE 0 END) AS completed, " +
                    "SUM(CASE WHEN Status = 'Missed' OR NoShowFlag = TRUE THEN 1 ELSE 0 END) AS missed, " +
                    "SUM(CASE WHEN Status = 'Rescheduled' THEN 1 ELSE 0 END) AS rescheduled, " +
                    "SUM(CASE WHEN Status = 'Patient_Cancelled' THEN 1 ELSE 0 END) AS patient_cancelled, " +
                    "SUM(CASE WHEN Status = 'Clinic_Cancelled' THEN 1 ELSE 0 END) AS clinic_cancelled " +
                    "FROM rendezvous " +
                    "WHERE DATE(AppointmentDateTime) BETWEEN ? AND ?";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                int completed = rs.getInt("completed");
                int missed = rs.getInt("missed");
                int rescheduled = rs.getInt("rescheduled");
                int patientCancelled = rs.getInt("patient_cancelled");
                int clinicCancelled = rs.getInt("clinic_cancelled");

                stats.put("total", total);
                stats.put("completed", completed);
                stats.put("missed", missed);
                stats.put("rescheduled", rescheduled);
                stats.put("patientCancelled", patientCancelled);
                stats.put("clinicCancelled", clinicCancelled);

                // Calculate rates
                if (total > 0) {
                    stats.put("completionRate", (double) completed / total);
                    stats.put("noShowRate", (double) missed / total);
                    stats.put("cancellationRate", (double) (patientCancelled + clinicCancelled) / total);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Get today's appointment statistics
     */
    public Map<String, Object> getTodayStats() {
        LocalDate today = LocalDate.now();
        return getAppointmentStats(today, today);
    }

    /**
     * Get next available time slots for a doctor
     */
    public List<LocalDateTime> getAvailableTimeSlots(String doctorId, LocalDate date) {
        List<LocalDateTime> availableSlots = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // First, generate all possible time slots (e.g., every 30 minutes from 8 AM to 5 PM)
            List<LocalDateTime> allSlots = new ArrayList<>();
            LocalDateTime startTime = date.atTime(8, 0); // 8:00 AM
            LocalDateTime endTime = date.atTime(17, 0);  // 5:00 PM

            LocalDateTime current = startTime;
            while (current.isBefore(endTime)) {
                allSlots.add(current);
                current = current.plusMinutes(30);
            }

            // Then, get all booked slots for the doctor on that date
            String query = "SELECT AppointmentDateTime FROM rendezvous " +
                    "WHERE MedecinID = ? AND DATE(AppointmentDateTime) = ? " +
                    "AND Status NOT IN ('Completed', 'Missed', 'Patient_Cancelled', 'Clinic_Cancelled')";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, doctorId);
            pstmt.setDate(2, java.sql.Date.valueOf(date));

            ResultSet rs = pstmt.executeQuery();

            List<LocalDateTime> bookedSlots = new ArrayList<>();
            while (rs.next()) {
                bookedSlots.add(rs.getTimestamp("AppointmentDateTime").toLocalDateTime());
            }

            // Find available slots (slots not in bookedSlots)
            for (LocalDateTime slot : allSlots) {
                boolean isAvailable = true;
                for (LocalDateTime booked : bookedSlots) {
                    // Consider a slot booked if it's within 30 minutes of a booked appointment
                    if (Math.abs(slot.until(booked, java.time.temporal.ChronoUnit.MINUTES)) < 30) {
                        isAvailable = false;
                        break;
                    }
                }

                if (isAvailable) {
                    availableSlots.add(slot);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return availableSlots;
    }

    /**
     * Helper method to create an Appointment object from a ResultSet
     */
    private Appointment createAppointmentFromResultSet(ResultSet rs) throws SQLException {
        int rendezVousID = rs.getInt("RendezVousID");
        String patientID = rs.getString("PatientID");
        String medicinID = rs.getString("MedecinID");
        LocalDateTime appointmentDateTime = rs.getTimestamp("AppointmentDateTime").toLocalDateTime();
        String reasonForVisit = rs.getString("ReasonForVisit");
        String status = rs.getString("Status");

        Appointment appointment = new Appointment();
        appointment.setRendezVousID(rendezVousID);
        appointment.setPatientID(patientID);
        appointment.setMedicinID(medicinID);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setReasonForVisit(reasonForVisit);
        appointment.setStatus(status);

        // Patient and doctor names
        String patientFName = rs.getString("patientFName");
        String patientLName = rs.getString("patientLName");
        String doctorFName = rs.getString("doctorFName");
        String doctorLName = rs.getString("doctorLName");

        if (patientFName != null && patientLName != null) {
            appointment.setPatientName(patientFName + " " + patientLName);
        }

        if (doctorFName != null && doctorLName != null) {
            appointment.setDoctorName(doctorFName + " " + doctorLName);
        }

        // Additional fields
        try {
            appointment.setStatusReason(rs.getString("StatusReason"));
            appointment.setNoShowFlag(rs.getBoolean("NoShowFlag"));

            int rescheduledToID = rs.getInt("RescheduledToID");
            if (!rs.wasNull()) {
                appointment.setRescheduledToID(rescheduledToID);
            }

            Timestamp cancellationTime = rs.getTimestamp("CancellationTime");
            if (cancellationTime != null) {
                appointment.setCancellationTime(cancellationTime.toLocalDateTime());
            }

            String priority = rs.getString("Priority");
            if (priority != null) {
                appointment.setPriority(priority);
            } else {
                appointment.setPriority("Normal");
            }
        } catch (SQLException e) {
            // Handle case where columns might not exist yet
            appointment.setPriority("Normal");
        }

        return appointment;
    }

    /**
     * Log a status change in the history table
     */
    private void logStatusChange(int appointmentId, String oldStatus, String newStatus, String reason, String username) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "INSERT INTO rendezvous_history " +
                    "(RendezVousID, PreviousStatus, NewStatus, StatusReason, ChangedBy, ChangedAt) " +
                    "VALUES (?, ?, ?, ?, ?, NOW())";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, appointmentId);

            if (oldStatus != null) {
                pstmt.setString(2, oldStatus);
            } else {
                pstmt.setNull(2, java.sql.Types.VARCHAR);
            }

            pstmt.setString(3, newStatus);
            pstmt.setString(4, reason);
            pstmt.setString(5, username);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}