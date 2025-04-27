package com.example.myjavafxapp.Models.patient;

import com.example.myjavafxapp.Models.util.DatabaseSingleton;
import com.example.myjavafxapp.Models.appointment.Appointment;
import com.example.myjavafxapp.Models.appointment.AppointmentManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientManager {
    private static PatientManager instance;

    private PatientManager() {}

    public static synchronized PatientManager getInstance() {
        if (instance == null) {
            instance = new PatientManager();
        }
        return instance;
    }

    /**
     * Search for patients by ID, name, or other criteria
     */
    public List<Patient> searchPatients(String searchTerm) {
        List<Patient> patients = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Create a search query that looks for matches in ID, first name, last name
            String query = "SELECT * FROM patient WHERE " +
                    "ID LIKE ? OR " +
                    "FNAME LIKE ? OR " +
                    "LNAME LIKE ? OR " +
                    "TELEPHONE LIKE ? OR " +
                    "EMAIL LIKE ? " +
                    "ORDER BY FNAME, LNAME";

            String searchParam = "%" + searchTerm + "%";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, searchParam);
            pstmt.setString(2, searchParam);
            pstmt.setString(3, searchParam);
            pstmt.setString(4, searchParam);
            pstmt.setString(5, searchParam);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Patient patient = createPatientFromResultSet(rs);
                patients.add(patient);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return patients;
    }

    /**
     * Get all patients
     */
    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM patient ORDER BY FNAME, LNAME";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Patient patient = createPatientFromResultSet(rs);
                patients.add(patient);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return patients;
    }

    /**
     * Get a patient by ID
     */
    public Patient getPatientById(String patientId) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "SELECT * FROM patient WHERE ID = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, patientId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return createPatientFromResultSet(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Create a new patient
     */
    public boolean createPatient(Patient patient) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "INSERT INTO patient (ID, FNAME, LNAME, BIRTHDATE, SEXE, ADRESSE, TELEPHONE, EMAIL, CREATED_AT) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, patient.getID());
            pstmt.setString(2, patient.getFNAME());
            pstmt.setString(3, patient.getLNAME());
            pstmt.setDate(4, java.sql.Date.valueOf(patient.getBIRTHDATE()));
            pstmt.setString(5, patient.getSEXE());
            pstmt.setString(6, patient.getADRESSE());
            pstmt.setInt(7, patient.getTELEPHONE());
            pstmt.setString(8, patient.getEMAIL());

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing patient
     */
    public boolean updatePatient(Patient patient) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            String query = "UPDATE patient SET " +
                    "FNAME = ?, LNAME = ?, BIRTHDATE = ?, SEXE = ?, " +
                    "ADRESSE = ?, TELEPHONE = ?, EMAIL = ? " +
                    "WHERE ID = ?";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, patient.getFNAME());
            pstmt.setString(2, patient.getLNAME());
            pstmt.setDate(3, java.sql.Date.valueOf(patient.getBIRTHDATE()));
            pstmt.setString(4, patient.getSEXE());
            pstmt.setString(5, patient.getADRESSE());
            pstmt.setInt(6, patient.getTELEPHONE());
            pstmt.setString(7, patient.getEMAIL());
            pstmt.setString(8, patient.getID());

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a patient
     */
    public boolean deletePatient(String patientId) {
        Connection conn = DatabaseSingleton.getInstance().getConnection();

        try {
            // Check if patient has appointments
            String checkQuery = "SELECT COUNT(*) FROM rendezvous WHERE PatientID = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, patientId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                // Patient has appointments, cannot delete
                return false;
            }

            // Check if patient has medical records
            String checkDossierQuery = "SELECT COUNT(*) FROM dossierpatient WHERE PatientID = ?";
            PreparedStatement checkDossierStmt = conn.prepareStatement(checkDossierQuery);
            checkDossierStmt.setString(1, patientId);
            ResultSet dossierRs = checkDossierStmt.executeQuery();

            if (dossierRs.next() && dossierRs.getInt(1) > 0) {
                // Patient has a medical record, cannot delete
                return false;
            }

            // Delete the patient
            String deleteQuery = "DELETE FROM patient WHERE ID = ?";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
            deleteStmt.setString(1, patientId);

            int result = deleteStmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get a patient's appointment history
     */
    public List<Appointment> getPatientAppointmentHistory(String patientId) {
        AppointmentManager appointmentManager = AppointmentManager.getInstance();
        return appointmentManager.getAppointmentsByPatient(patientId);
    }

    /**
     * Helper method to create a Patient object from a ResultSet
     */
    private Patient createPatientFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("ID");
        String firstName = rs.getString("FNAME");
        String lastName = rs.getString("LNAME");

        // Parse date
        LocalDate birthdate = null;
        Date birthdateSQL = rs.getDate("BIRTHDATE");
        if (birthdateSQL != null) {
            birthdate = birthdateSQL.toLocalDate();
        }

        String sexe = rs.getString("SEXE");
        String adresse = rs.getString("ADRESSE");
        int telephone = rs.getInt("TELEPHONE");
        String email = rs.getString("EMAIL");

        // Convert the parsed date to a string for the constructor
        String birthdateStr = birthdate != null ? birthdate.toString() : "";

        return new Patient(id, firstName, lastName, birthdateStr, sexe, adresse, telephone, email);
    }
}