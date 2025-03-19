package com.example.myjavafxapp.Models;

import java.sql.Timestamp;

public class DossierPatient {
    private int dossierID;
    private String patientID;
    private String bloodType;
    private String allergies;
    private String medicalHistory;
    private String currentMedications;
    private Timestamp dateCreated;
    private Timestamp lastUpdated;
    private String bloodPressure;
    private String chronicConditions;
    private String previousSurgeries;
    private String familyMedicalHistory;
    private String insuranceProvider;
    private String insurancePolicyNumber;

    public DossierPatient() {
        // Default constructor
    }

    public DossierPatient(int dossierID, String patientID, String bloodType, String allergies,
                          String medicalHistory, String currentMedications, Timestamp dateCreated,
                          Timestamp lastUpdated, String bloodPressure, String chronicConditions,
                          String previousSurgeries, String familyMedicalHistory,
                          String insuranceProvider, String insurancePolicyNumber) {
        this.dossierID = dossierID;
        this.patientID = patientID;
        this.bloodType = bloodType;
        this.allergies = allergies;
        this.medicalHistory = medicalHistory;
        this.currentMedications = currentMedications;
        this.dateCreated = dateCreated;
        this.lastUpdated = lastUpdated;
        this.bloodPressure = bloodPressure;
        this.chronicConditions = chronicConditions;
        this.previousSurgeries = previousSurgeries;
        this.familyMedicalHistory = familyMedicalHistory;
        this.insuranceProvider = insuranceProvider;
        this.insurancePolicyNumber = insurancePolicyNumber;
    }

    // Getters and Setters
    public int getDossierID() {
        return dossierID;
    }

    public void setDossierID(int dossierID) {
        this.dossierID = dossierID;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getCurrentMedications() {
        return currentMedications;
    }

    public void setCurrentMedications(String currentMedications) {
        this.currentMedications = currentMedications;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public String getChronicConditions() {
        return chronicConditions;
    }

    public void setChronicConditions(String chronicConditions) {
        this.chronicConditions = chronicConditions;
    }

    public String getPreviousSurgeries() {
        return previousSurgeries;
    }

    public void setPreviousSurgeries(String previousSurgeries) {
        this.previousSurgeries = previousSurgeries;
    }

    public String getFamilyMedicalHistory() {
        return familyMedicalHistory;
    }

    public void setFamilyMedicalHistory(String familyMedicalHistory) {
        this.familyMedicalHistory = familyMedicalHistory;
    }

    public String getInsuranceProvider() {
        return insuranceProvider;
    }

    public void setInsuranceProvider(String insuranceProvider) {
        this.insuranceProvider = insuranceProvider;
    }

    public String getInsurancePolicyNumber() {
        return insurancePolicyNumber;
    }

    public void setInsurancePolicyNumber(String insurancePolicyNumber) {
        this.insurancePolicyNumber = insurancePolicyNumber;
    }
}