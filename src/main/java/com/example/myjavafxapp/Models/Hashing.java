package com.example.myjavafxapp.Models;

import org.mindrot.jbcrypt.BCrypt;

public class Hashing {

    // Method that turns a plain password to a Hashed one
    public static String hashPassword(String plainPassword) {

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // Method that checks if the plain password matches the hashed password
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {

        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

}
