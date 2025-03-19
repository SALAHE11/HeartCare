package com.example.myjavafxapp.Models;

public class PatientDataHolder {
    private static PatientDataHolder instance;
    private Patient currentPatient;
    private DossierPatient currentDossier;

    private PatientDataHolder() {
        // Private constructor
    }

    public static synchronized PatientDataHolder getInstance() {
        if (instance == null) {
            instance = new PatientDataHolder();
        }
        return instance;
    }

    public Patient getCurrentPatient() {
        return currentPatient;
    }

    public void setCurrentPatient(Patient currentPatient) {
        this.currentPatient = currentPatient;
    }

    public DossierPatient getCurrentDossier() {
        return currentDossier;
    }

    public void setCurrentDossier(DossierPatient currentDossier) {
        this.currentDossier = currentDossier;
    }
}