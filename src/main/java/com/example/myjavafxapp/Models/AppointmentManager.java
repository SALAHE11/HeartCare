package com.example.myjavafxapp.Models;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
     * Check if a patient has overlapping appointments
     * Returns true if there is an overlap, false otherwise
     */
    private boolean hasPatientOverlappingAppointment(String patientID, LocalDateTime dateTime, Integer excludeAppointmentId) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        boolean hasOverlap = false;

        try {
            // Build query to check for overlapping appointments within a 15-minute window
            String query = "SELECT COUNT(*) FROM rendezvous " +
                    "WHERE PatientID = ? " +
                    "AND ABS(TIMESTAMPDIFF(MINUTE, AppointmentDateTime, ?)) < 15 " + // 15-minute window
                    "AND Status NOT IN ('Completed', 'Missed', 'Patient_Cancelled', 'Clinic_Cancelled')";

            // If updating an existing appointment, exclude it from the check
            if (excludeAppointmentId != null) {
                query += " AND RendezVousID != ?";
            }

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, patientID);
            pstmt.setTimestamp(2, Timestamp.valueOf(dateTime));

            if (excludeAppointmentId != null) {
                pstmt.setInt(3, excludeAppointmentId);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                hasOverlap = true;
            }
        } catch (SQLException e) {
            System.err.println("Error checking for overlapping appointments: " + e.getMessage());
            e.printStackTrace();
        }

        return hasOverlap;
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

            // Check if patient already has an appointment at the same time
            if (hasPatientOverlappingAppointment(appointment.getPatientID(), appointment.getAppointmentDateTime(), null)) {
                System.err.println("Patient already has an appointment at this time");
                throw new SQLException("Patient already has an appointment at this time");
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

            // Check if patient already has an appointment at the same time (excluding this one)
            if (hasPatientOverlappingAppointment(appointment.getPatientID(), appointment.getAppointmentDateTime(),
                    appointment.getRendezVousID())) {
                System.err.println("Patient already has an appointment at this time");
                throw new SQLException("Patient already has an appointment at this time");
            }

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
            return false;
        }
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

            // Check if patient already has an appointment at the new time
            if (hasPatientOverlappingAppointment(oldAppointment.getPatientID(), newDateTime, oldAppointmentId)) {
                System.err.println("Patient already has an appointment at the new time");
                conn.rollback();
                throw new SQLException("Patient already has an appointment at the new time");
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
            // First, generate all possible time slots (e.g., every 15 minutes from 8 AM to 5 PM)
            List<LocalDateTime> allSlots = new ArrayList<>();
            LocalDateTime startTime = date.atTime(8, 0); // 8:00 AM
            LocalDateTime endTime = date.atTime(17, 45);  // 5:45 PM (changed from 5:00 PM)

            LocalDateTime current = startTime;
            while (current.isBefore(endTime) || current.equals(endTime)) {
                allSlots.add(current);
                current = current.plusMinutes(15); // Changed from 30 to 15 minutes
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
                    // Consider a slot booked if it's within 15 minutes of a booked appointment (changed from 30)
                    if (Math.abs(slot.until(booked, java.time.temporal.ChronoUnit.MINUTES)) < 15) {
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

    /**
     * Changes the time of an existing appointment
     *
     * @param appointmentId The ID of the appointment to update
     * @param newDateTime The new date and time for the appointment
     * @param reason Optional reason for the time change
     * @return true if successful, false otherwise
     */
    public boolean changeAppointmentTime(int appointmentId, LocalDateTime newDateTime, String reason) {
        try {
            // Get a reference to the appointment in the database
            Appointment appointment = getAppointmentById(appointmentId);
            if (appointment == null) {
                System.out.println("Appointment not found: " + appointmentId);
                return false;
            }

            // Save the old time for logging
            LocalDateTime oldDateTime = appointment.getAppointmentDateTime();

            // Update the appointment time
            appointment.setAppointmentDateTime(newDateTime);

            // Add notes about the time change if a reason was provided
            if (reason != null && !reason.isEmpty()) {
                String existingNotes = appointment.getNotes();
                String timeChangeNote = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "] " +
                        "Rendez-vous avancé de " + oldDateTime.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        " à " + newDateTime.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        ". Raison: " + reason;

                // Append to existing notes or create new notes
                if (existingNotes != null && !existingNotes.isEmpty()) {
                    appointment.setNotes(existingNotes + "\n\n" + timeChangeNote);
                } else {
                    appointment.setNotes(timeChangeNote);
                }
            }

            // Save the updated appointment to the database
            boolean updated = updateAppointment(appointment);

            // Log the time change
            if (updated) {
                System.out.println("Appointment time changed: " + appointmentId +
                        " from " + oldDateTime + " to " + newDateTime);

                // You might want to add code here to notify relevant parties about the change
            }

            return updated;
        } catch (Exception e) {
            System.err.println("Error changing appointment time: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Schedules an urgent appointment and shifts other appointments as needed.
     * This will place the appointment after the last Completed or InProgress appointment,
     * or as the first appointment of the day if none exists. All subsequent appointments
     * will be shifted by 15 minutes.
     *
     * @param urgentAppointment The urgent appointment to be scheduled
     * @return true if successful, false otherwise
     */
    public boolean scheduleUrgentAppointment(Appointment urgentAppointment) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();
        try {
            // Set auto-commit to false for transaction
            conn.setAutoCommit(false);

            // Get the date and doctor ID
            LocalDate appointmentDate = urgentAppointment.getAppointmentDateTime().toLocalDate();
            String doctorId = urgentAppointment.getMedicinID();

            // Get all appointments for this doctor on this date
            List<Appointment> appointments = getAppointmentsByDate(appointmentDate);
            List<Appointment> doctorAppointments = appointments.stream()
                    .filter(a -> a.getMedicinID().equals(doctorId))
                    .collect(Collectors.toList());

            // Find the latest completed or in-progress appointment
            doctorAppointments.sort(Comparator.comparing(Appointment::getAppointmentDateTime));

            Appointment latestActiveAppointment = null;
            for (Appointment app : doctorAppointments) {
                if ("Completed".equals(app.getStatus()) || "InProgress".equals(app.getStatus())) {
                    latestActiveAppointment = app;
                }
            }

            // Determine the insertion time
            LocalDateTime insertionTime;
            if (latestActiveAppointment != null) {
                // Place after the latest active appointment
                insertionTime = latestActiveAppointment.getAppointmentDateTime().plusMinutes(15);
            } else {
                // Place at the start of the day (8:00 AM)
                insertionTime = appointmentDate.atTime(8, 0);
            }

            // Set the appointment time
            urgentAppointment.setAppointmentDateTime(insertionTime);

            // Check if we need to shift any appointments at this time
            boolean timeOccupied = doctorAppointments.stream()
                    .anyMatch(a -> a.getAppointmentDateTime().equals(insertionTime) &&
                            !("Completed".equals(a.getStatus()) || "InProgress".equals(a.getStatus())));

            if (timeOccupied) {
                // Shift appointments at or after the insertion time
                List<Appointment> appointmentsToShift = doctorAppointments.stream()
                        .filter(a -> !("Completed".equals(a.getStatus()) || "InProgress".equals(a.getStatus())))
                        .filter(a -> a.getAppointmentDateTime().compareTo(insertionTime) >= 0)
                        .sorted(Comparator.comparing(Appointment::getAppointmentDateTime).reversed())
                        .collect(Collectors.toList());

                // Shift each appointment by 15 minutes
                for (Appointment appointment : appointmentsToShift) {
                    LocalDateTime newTime = appointment.getAppointmentDateTime().plusMinutes(15);

                    // Check clinic hours (8:00 AM to 5:45 PM)
                    if (newTime.isAfter(appointmentDate.atTime(17, 45))) {
                        // Cannot shift beyond clinic hours
                        System.out.println("Warning: Cannot shift appointment " + appointment.getRendezVousID() +
                                " as it would go beyond clinic hours.");
                        continue;
                    }

                    boolean shiftSuccess = changeAppointmentTime(
                            appointment.getRendezVousID(),
                            newTime,
                            "Décalé pour accommoder un rendez-vous urgent"
                    );

                    if (!shiftSuccess) {
                        System.out.println("Failed to shift appointment: " + appointment.getRendezVousID());
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Insert the urgent appointment
            boolean success = createAppointment(urgentAppointment);
            if (!success) {
                conn.rollback();
                return false;
            }

            // Commit the transaction
            conn.commit();
            return true;

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("Error scheduling urgent appointment: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}