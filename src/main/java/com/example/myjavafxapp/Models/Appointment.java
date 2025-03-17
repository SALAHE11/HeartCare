package com.example.myjavafxapp.Models;

public class Appointment {
    private int rendezVousID;
    private String patientID;
    private String medicinID;
    private String appointmentDateTime;
    private String reasonForVisit;
    private String status;

    public Appointment(int rendezVousID, String patientID, String medicinID, String appointmentDateTime, String reasonForVisit, String status) {
        this.rendezVousID = rendezVousID;
        this.patientID = patientID;
        this.medicinID = medicinID;
        this.appointmentDateTime = appointmentDateTime;
        this.reasonForVisit = reasonForVisit;
        this.status = status;
    }

    // Getters and setters
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

    public String getMedicinID() {
        return medicinID;
    }

    public void setMedicinID(String medicinID) {
        this.medicinID = medicinID;
    }

    public String getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(String appointmentDateTime) {
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
}