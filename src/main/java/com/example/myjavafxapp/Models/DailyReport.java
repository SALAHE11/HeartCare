package com.example.myjavafxapp.Models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class to encapsulate the data for a daily report
 */
public class DailyReport {
    private LocalDate reportDate;
    private int totalAppointments;
    private int completedAppointments;
    private int canceledAppointments;
    private List<PatientVisit> patientsVisits;
    private Map<LocalTime, Integer> appointmentsByHour;
    private double totalRevenue;
    private Map<String, Double> revenueByPaymentMethod;
    private Map<String, Integer> patientsByDoctor;
    private boolean isPartial;

    /**
     * Inner class to represent a patient visit with detailed information
     */
    public static class PatientVisit {
        private String cin;
        private String firstName;
        private String lastName;
        private LocalDateTime visitTime;
        private String paymentMethod;

        public PatientVisit(String cin, String firstName, String lastName, LocalDateTime visitTime, String paymentMethod) {
            this.cin = cin;
            this.firstName = firstName;
            this.lastName = lastName;
            this.visitTime = visitTime;
            this.paymentMethod = paymentMethod;
        }

        public String getCin() {
            return cin;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public LocalDateTime getVisitTime() {
            return visitTime;
        }

        public String getPaymentMethod() {
            return paymentMethod != null ? paymentMethod : "Non spécifié";
        }

        @Override
        public String toString() {
            return firstName + " " + lastName + " (CIN: " + cin + ")";
        }
    }

    // Constructor
    public DailyReport(LocalDate reportDate) {
        this.reportDate = reportDate;
        this.patientsVisits = new ArrayList<>();
        this.appointmentsByHour = new HashMap<>();
        this.revenueByPaymentMethod = new HashMap<>();
        this.patientsByDoctor = new HashMap<>();
        this.isPartial = false;
    }

    // Getters and setters
    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public int getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(int totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public int getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(int completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public int getCanceledAppointments() {
        return canceledAppointments;
    }

    public void setCanceledAppointments(int canceledAppointments) {
        this.canceledAppointments = canceledAppointments;
    }

    public List<PatientVisit> getPatientsVisits() {
        return patientsVisits;
    }

    public void setPatientsVisits(List<PatientVisit> patientsVisits) {
        this.patientsVisits = patientsVisits;
    }

    public Map<LocalTime, Integer> getAppointmentsByHour() {
        return appointmentsByHour;
    }

    public void setAppointmentsByHour(Map<LocalTime, Integer> appointmentsByHour) {
        this.appointmentsByHour = appointmentsByHour;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Map<String, Double> getRevenueByPaymentMethod() {
        return revenueByPaymentMethod;
    }

    public void setRevenueByPaymentMethod(Map<String, Double> revenueByPaymentMethod) {
        this.revenueByPaymentMethod = revenueByPaymentMethod;
    }

    public Map<String, Integer> getPatientsByDoctor() {
        return patientsByDoctor;
    }

    public void setPatientsByDoctor(Map<String, Integer> patientsByDoctor) {
        this.patientsByDoctor = patientsByDoctor;
    }

    public boolean isPartial() {
        return isPartial;
    }

    public void setPartial(boolean partial) {
        isPartial = partial;
    }

    /**
     * Helper method to find the peak hour for appointments
     * @return The hour with the most appointments
     */
    public LocalTime getPeakHour() {
        if (appointmentsByHour.isEmpty()) {
            return null;
        }

        LocalTime peakHour = null;
        int maxAppointments = 0;

        for (Map.Entry<LocalTime, Integer> entry : appointmentsByHour.entrySet()) {
            if (entry.getValue() > maxAppointments) {
                maxAppointments = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        return peakHour;
    }
}