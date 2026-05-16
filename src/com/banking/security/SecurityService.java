package com.banking.security;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

/**
 * Module 5 - Security Service.
 *
 * Provides:
 *  - SHA-256 + salt password hashing  (BCrypt would require an external lib,
 *    so we use MessageDigest which is available in the JDK)
 *  - RSA key-pair generation, encryption, and decryption
 *  - Account-number masking for logs/receipts
 */
public class SecurityService {

    private static KeyPair rsaKeyPair;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            rsaKeyPair = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA key generation failed", e);
        }
    }

    // -- Password Hashing -----------------------------------------------------

    /**
     * Hashes a plain-text password using SHA-256.
     * A fixed application-level salt is prepended for extra protection.
     */
    public static String hashPassword(String plainPassword) {
        try {
            String salted = "BankSalt$2024#" + plainPassword;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(salted.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /** Returns true if the plain password matches the stored hash. */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        return hashPassword(plainPassword).equals(storedHash);
    }

    // -- RSA Encryption / Decryption ------------------------------------------

    /** Encrypts data with the RSA public key; returns Base64-encoded ciphertext. */
    public static String encryptRSA(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA encryption failed", e);
        }
    }

    /** Decrypts Base64-encoded ciphertext with the RSA private key. */
    public static String decryptRSA(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("RSA decryption failed", e);
        }
    }

    // -- Data Masking ---------------------------------------------------------

    /**
     * Masks all but the last 4 digits of an account number.
     * e.g. "1234567890" -> "******7890"
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        int visible = 4;
        String mask = "*".repeat(accountNumber.length() - visible);
        return mask + accountNumber.substring(accountNumber.length() - visible);
    }

    /** Masks an email: j***@example.com */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 1) return "*" + domain;
        return local.charAt(0) + "*".repeat(local.length() - 1) + domain;
    }

    public static PublicKey  getPublicKey()  { return rsaKeyPair.getPublic(); }
    public static PrivateKey getPrivateKey() { return rsaKeyPair.getPrivate(); }
}
