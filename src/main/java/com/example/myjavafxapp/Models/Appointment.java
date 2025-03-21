package com.example.myjavafxapp.Models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment {
    private int rendezVousID;
    private String patientID;
    private String patientName; // Calculated field (not in DB)
    private String medicinID;
    private String doctorName; // Calculated field (not in DB)
    private LocalDateTime appointmentDateTime;
    private String reasonForVisit;
    private String status;
    private String statusReason;
    private boolean noShowFlag;
    private Integer rescheduledToID;
    private LocalDateTime cancellationTime;
    private String priority;
    private String notes;

    // Default constructor
    public Appointment() {
        this.status = "Scheduled";
        this.priority = "Normal";
        this.noShowFlag = false;
        this.appointmentDateTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        this.statusReason = ""; // Initialize with empty string instead of null
        this.notes = ""; // Initialize with empty string instead of null
    }

    // Constructor with all fields
    public Appointment(int rendezVousID, String patientID, String medicinID,
                       LocalDateTime appointmentDateTime, String reasonForVisit,
                       String status, String statusReason, boolean noShowFlag,
                       Integer rescheduledToID, LocalDateTime cancellationTime,
                       String priority, String notes) {
        this.rendezVousID = rendezVousID;
        this.patientID = patientID;
        this.medicinID = medicinID;
        this.appointmentDateTime = appointmentDateTime;
        this.reasonForVisit = reasonForVisit;
        this.status = status;
        this.statusReason = statusReason;
        this.noShowFlag = noShowFlag;
        this.rescheduledToID = rescheduledToID;
        this.cancellationTime = cancellationTime;
        this.priority = priority;
        this.notes = notes;
    }

    // Simplified constructor for backward compatibility
    public Appointment(int rendezVousID, String patientID, String medicinID,
                       String appointmentDateTimeStr, String reasonForVisit, String status) {
        this.rendezVousID = rendezVousID;
        this.patientID = patientID;
        this.medicinID = medicinID;
        this.reasonForVisit = reasonForVisit;
        this.status = status;
        this.priority = "Normal";
        this.noShowFlag = false;

        // Parse date string to LocalDateTime
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            this.appointmentDateTime = LocalDateTime.parse(appointmentDateTimeStr, formatter);
        } catch (Exception e) {
            this.appointmentDateTime = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public int getRendezVousID() {
        return rendezVousID;
    }

    public void setRendezVousID(int rendezVousID) {
        this.rendezVousID = rendezVousID;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getMedicinID() {
        return medicinID;
    }

    public void setMedicinID(String medicinID) {
        this.medicinID = medicinID;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public String getFormattedDateTime() {
        if (appointmentDateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return appointmentDateTime.format(formatter);
    }

    public String getFormattedDate() {
        if (appointmentDateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return appointmentDateTime.format(formatter);
    }

    public String getFormattedTime() {
        if (appointmentDateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return appointmentDateTime.format(formatter);
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public String getReasonForVisit() {
        return reasonForVisit;
    }

    public void setReasonForVisit(String reasonForVisit) {
        this.reasonForVisit = reasonForVisit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public boolean isNoShowFlag() {
        return noShowFlag;
    }

    public void setNoShowFlag(boolean noShowFlag) {
        this.noShowFlag = noShowFlag;
    }

    public Integer getRescheduledToID() {
        return rescheduledToID;
    }

    public void setRescheduledToID(Integer rescheduledToID) {
        this.rescheduledToID = rescheduledToID;
    }

    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }

    public void setCancellationTime(LocalDateTime cancellationTime) {
        this.cancellationTime = cancellationTime;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper method to determine if appointment is in the future
    public boolean isFuture() {
        return appointmentDateTime != null && appointmentDateTime.isAfter(LocalDateTime.now());
    }

    // Helper method to determine if appointment is in the past
    public boolean isPast() {
        return appointmentDateTime != null && appointmentDateTime.isBefore(LocalDateTime.now());
    }

    // Helper method to determine if appointment is active (scheduled, checked-in, in-progress)
    public boolean isActive() {
        return "Scheduled".equals(status) || "CheckedIn".equals(status) || "InProgress".equals(status);
    }

    // Helper method to determine if appointment is urgent
    public boolean isUrgent() {
        return "Urgent".equals(priority);
    }
}