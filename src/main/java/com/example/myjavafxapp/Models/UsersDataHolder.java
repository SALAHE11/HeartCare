package com.example.myjavafxapp.Models;
import java.awt.*;
public class UsersDataHolder {
    private static UsersDataHolder instance;
    private Users currentUsers;


    public static UsersDataHolder getInstance() {
        if (instance == null) {
            instance = new UsersDataHolder();
        }
        return instance;
    }

    public void setCurrent(Users user) {
        this.currentUsers = currentUsers;
    }
    public Users getCurrentUsers() {
        return currentUsers;
    }


}
