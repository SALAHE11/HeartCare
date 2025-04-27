package com.example.myjavafxapp.Models.patient;

public class Patient {
    private String ID;
    private String FNAME;
    private String LNAME;
    private String BIRTHDATE;
    private String SEXE;
    private String ADRESSE;
    private Integer TELEPHONE;
    private String EMAIL;

    public Patient(String ID, String FNAME, String LNAME, String BIRTHDATE, String SEXE, String ADRESSE, Integer TELEPHONE, String EMAIL) {
        this.ID = ID;
        this.FNAME = FNAME;
        this.LNAME = LNAME;
        this.BIRTHDATE = BIRTHDATE;
        this.SEXE = SEXE;
        this.ADRESSE = ADRESSE;
        this.TELEPHONE = TELEPHONE;
        this.EMAIL = EMAIL;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getFNAME() {
        return FNAME;
    }

    public void setFNAME(String FNAME) {
        this.FNAME = FNAME;
    }

    public String getLNAME() {
        return LNAME;
    }

    public void setLNAME(String LNAME) {
        this.LNAME = LNAME;
    }

    public String getBIRTHDATE() {
        return BIRTHDATE;
    }

    public void setBIRTHDATE(String BIRTHDATE) {
        this.BIRTHDATE = BIRTHDATE;
    }

    public String getSEXE() {
        return SEXE;
    }

    public void setSEXE(String SEXE) {
        this.SEXE = SEXE;
    }

    public String getADRESSE() {
        return ADRESSE;
    }

    public void setADRESSE(String ADRESSE) {
        this.ADRESSE = ADRESSE;
    }

    public Integer getTELEPHONE() {
        return TELEPHONE;
    }

    public void setTELEPHONE(Integer TELEPHONE) {
        this.TELEPHONE = TELEPHONE;
    }

    public String getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(String EMAIL) {
        this.EMAIL = EMAIL;
    }
}