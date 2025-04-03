package com.example.myjavafxapp.Models;

import java.awt.*;

public class Users extends Checkbox {

    private String ID;
    private String USERNAME;
    private String PASSWORD;
    private String FNAME;
    private String LNAME;
    private String EMAIL;
    private int TELEPHONE;
    private String ADRESSE;
    private String BIRTHDATE;
    private String ROLE;


    public Users(String ID, String FNAME, String LNAME, String BIRTHDATE,
                 String ROLE, String USERNAME, String EMAIL) {
        this.ID = ID;
        this.FNAME = FNAME;
        this.LNAME = LNAME;
        this.BIRTHDATE = BIRTHDATE;
        this.ROLE = ROLE;
        this.USERNAME = USERNAME;
        this.EMAIL = EMAIL;
    }



    // Getters et Setters

    public String getID() {
        return ID;
    }

    public void setID(String ID ) {
        this.ID = ID ;
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    public void setUSERNAME(String USERNAME) {
        this.USERNAME = USERNAME;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    public void setPASSWORD(String PASSWORD) {
        this.PASSWORD = PASSWORD;
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

    public String getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(String EMAIL) {
        this.EMAIL = EMAIL;
    }

    public int getTELEPHONE() {
        return TELEPHONE;
    }

    public void setTELEPHONE(int TELEPHONE) {
        this.TELEPHONE = TELEPHONE;
    }

    public String getADRESSE() {
        return ADRESSE;
    }

    public void setADRESSE(String ADRESSE) {
        this.ADRESSE = ADRESSE;
    }

    public String getBIRTHDATE() {
        return BIRTHDATE;
    }

    public void setBIRTHDATE(String BIRTHDATE) {
        this.BIRTHDATE= BIRTHDATE;
    }

    public String getROLE() {
        return ROLE;
    }

    public void setROLE(String ROLE) {
        this.ROLE = ROLE;
    }

    @Override
    public String toString() {
        return FNAME + " " + LNAME;
    }

    // Helper methods
    public boolean isDoctor() {
        return "medecin".equals(ROLE);
    }

    public boolean isAdmin() {
        return "admin".equals(ROLE);
    }

    public boolean isStaff() {
        return "personnel".equals(ROLE);
    }
}
