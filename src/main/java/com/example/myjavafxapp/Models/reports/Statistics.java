package com.example.myjavafxapp.Models.reports;

public class Statistics {
    private int totalAppointments;
    private double noShowRate;
    private int registeredPatients;

    public Statistics(int totalAppointments, double noShowRate, int registeredPatients) {
        this.totalAppointments = totalAppointments;
        this.noShowRate = noShowRate;
        this.registeredPatients = registeredPatients;
    }

    // Getters and setters
    public int getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(int totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public double getNoShowRate() {
        return noShowRate;
    }

    public void setNoShowRate(double noShowRate) {
        this.noShowRate = noShowRate;
    }

    public int getRegisteredPatients() {
        return registeredPatients;
    }

    public void setRegisteredPatients(int registeredPatients) {
        this.registeredPatients = registeredPatients;
    }
}