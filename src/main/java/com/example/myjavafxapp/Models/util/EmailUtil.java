package com.example.myjavafxapp.Models.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailUtil {

    private static final String FROM_EMAIL = "salaheddinemoujahid1@gmail.com"; // Replace with your email
    private static final String FROM_PASSWORD = "vfjg jnil wvhg grsd"; // Replace with your email password
    private static final String SMTP_HOST = "smtp.gmail.com"; // Use appropriate SMTP server
    private static final String SMTP_PORT = "587"; // Use appropriate port

    // Expiration time in minutes
    private static final int CODE_EXPIRATION_MINUTES = 15;

    // Inner class to store code and timestamp
    private static class VerificationCodeInfo {
        String code;
        LocalDateTime timestamp;

        VerificationCodeInfo(String code) {
            this.code = code;
            this.timestamp = LocalDateTime.now();
        }

        boolean isExpired() {
            return ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now()) > CODE_EXPIRATION_MINUTES;
        }
    }

    // Stores temporary verification codes with usernames
    private static final ConcurrentHashMap<String, VerificationCodeInfo> verificationCodes = new ConcurrentHashMap<>();

    /**
     * Generates a random 6-digit verification code for the given username
     * @param username The username to generate a code for
     * @return The generated verification code
     */
    public static String generateVerificationCode(String username) {
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(1000000)); // 6-digit code

        // Store the code associated with the username along with timestamp
        verificationCodes.put(username, new VerificationCodeInfo(code));

        return code;
    }

    /**
     * Verifies if the provided code matches the stored code for the username
     * and checks if the code has not expired
     * @param username The username to check
     * @param code The code to verify
     * @return true if the code matches and is not expired, false otherwise
     */
    public static boolean verifyCode(String username, String code) {
        VerificationCodeInfo codeInfo = verificationCodes.get(username);

        if (codeInfo == null) {
            return false; // No code found for this username
        }

        if (codeInfo.isExpired()) {
            verificationCodes.remove(username); // Remove expired code
            return false; // Code has expired
        }

        return codeInfo.code.equals(code);
    }

    /**
     * Removes the verification code for a username after successful verification
     * @param username The username to clear the code for
     */
    public static void clearVerificationCode(String username) {
        verificationCodes.remove(username);
    }

    /**
     * Checks if a verification code exists for the username and if it's not expired
     * @param username The username to check
     * @return true if a valid, non-expired code exists, false otherwise
     */
    public static boolean hasValidCode(String username) {
        VerificationCodeInfo codeInfo = verificationCodes.get(username);
        return codeInfo != null && !codeInfo.isExpired();
    }

    /**
     * Returns the remaining validity time of a code in minutes
     * @param username The username to check
     * @return Remaining minutes before code expires, or -1 if no valid code exists
     */
    public static long getRemainingValidityMinutes(String username) {
        VerificationCodeInfo codeInfo = verificationCodes.get(username);
        if (codeInfo == null) {
            return -1;
        }

        long minutesPassed = ChronoUnit.MINUTES.between(codeInfo.timestamp, LocalDateTime.now());
        return Math.max(0, CODE_EXPIRATION_MINUTES - minutesPassed);
    }

    /**
     * Sends a password reset email with verification code
     * @param toEmail Recipient's email address
     * @param username Recipient's username
     * @param verificationCode The verification code
     * @return true if email sent successfully, false otherwise
     */
    public static boolean sendPasswordResetEmail(String toEmail, String username, String verificationCode) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Réinitialisation du mot de passe");

            String emailContent = "Bonjour " + username + ",\n\n" +
                    "Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte.\n" +
                    "Votre code de vérification est: " + verificationCode + "\n\n" +
                    "Ce code expirera dans " + CODE_EXPIRATION_MINUTES + " minutes.\n\n" +
                    "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe support";

            message.setText(emailContent);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}