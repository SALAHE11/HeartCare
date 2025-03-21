package com.example.myjavafxapp.Models;

import java.time.LocalDate;

public class User {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private String address;
    private int telephone;
    private LocalDate birthdate;

    public User() {
        // Default constructor
    }

    public User(String id, String username, String firstName, String lastName, String role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // Full constructor
    public User(String id, String username, String firstName, String lastName,
                String role, String address, int telephone, LocalDate birthdate) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.address = address;
        this.telephone = telephone;
        this.birthdate = birthdate;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTelephone() {
        return telephone;
    }

    public void setTelephone(int telephone) {
        this.telephone = telephone;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }

    // Helper methods
    public boolean isDoctor() {
        return "medecin".equals(role);
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isStaff() {
        return "personnel".equals(role);
    }
}