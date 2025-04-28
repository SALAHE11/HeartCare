package com.example.myjavafxapp.Models.payment;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Payment {
    private int paymentID;
    private String patientID;
    private int rendezVousID;
    private double amount;
    private String paymentMethod;
    private Timestamp paymentDate;

    // For UI display purposes
    private String patientName;
    private LocalDateTime appointmentDateTime;

    // For tracking changes and history
    private String changeReason;
    private Payment originalPayment;

    // Constructors
    public Payment() {}

    public Payment(int paymentID, String patientID, int rendezVousID, double amount,
                   String paymentMethod, Timestamp paymentDate) {
        this.paymentID = paymentID;
        this.patientID = patientID;
        this.rendezVousID = rendezVousID;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
    }

    // Getters and setters
    public int getPaymentID() { return paymentID; }
    public void setPaymentID(int paymentID) { this.paymentID = paymentID; }

    public String getPatientID() { return patientID; }
    public void setPatientID(String patientID) { this.patientID = patientID; }

    public int getRendezVousID() { return rendezVousID; }
    public void setRendezVousID(int rendezVousID) { this.rendezVousID = rendezVousID; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Timestamp getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Timestamp paymentDate) { this.paymentDate = paymentDate; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public LocalDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) { this.appointmentDateTime = appointmentDateTime; }

    // New methods for change tracking
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public Payment getOriginalPayment() { return originalPayment; }
    public void setOriginalPayment(Payment originalPayment) { this.originalPayment = originalPayment; }

    /**
     * Create a deep copy of this payment
     */
    public Payment clone() {
        Payment clone = new Payment();
        clone.setPaymentID(this.paymentID);
        clone.setPatientID(this.patientID);
        clone.setRendezVousID(this.rendezVousID);
        clone.setAmount(this.amount);
        clone.setPaymentMethod(this.paymentMethod);
        clone.setPaymentDate(this.paymentDate);
        clone.setPatientName(this.patientName);

        if (this.appointmentDateTime != null) {
            clone.setAppointmentDateTime(this.appointmentDateTime);
        }

        return clone;
    }
}