package com.example.myjavafxapp.Models.user;

public class UsersDataHolder {
    private static UsersDataHolder instance;
    private Users currentUsers;

    private UsersDataHolder() {
        // Private constructor
    }

    public static synchronized UsersDataHolder getInstance() {
        if (instance == null) {
            instance = new UsersDataHolder();
        }
        return instance;
    }

    public void setCurrentUser(Users user) {
        this.currentUsers = user;  // Fixed: assign parameter to field instead of self-assignment
    }

    public Users getCurrentUsers() {
        return currentUsers;
    }
}