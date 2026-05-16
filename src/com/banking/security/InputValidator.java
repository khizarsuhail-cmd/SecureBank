package com.banking.security;

/**
 * Module 5 - Input Validation.
 * Centralises all validation logic so no raw user input ever reaches
 * business logic without being checked first.
 */
public class InputValidator {

    // -- Regex constants -------------------------------------------------------
    private static final String EMAIL_REGEX    = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
    private static final String PHONE_REGEX    = "^\\+?[0-9]{10,13}$";
    private static final String NAME_REGEX     = "^[a-zA-Z ]{2,60}$";
    private static final String NATIONAL_REGEX = "^[0-9]{13}$";          // CNIC format
    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";

    // -- Validators ------------------------------------------------------------

    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_REGEX);
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches(NAME_REGEX);
    }

    public static boolean isValidNationalId(String id) {
        return id != null && id.matches(NATIONAL_REGEX);
    }

    /**
     * Password must be >=8 chars, contain at least one uppercase,
     * one lowercase, one digit, and one special character.
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_REGEX);
    }

    public static boolean isValidAmount(String input) {
        try {
            double v = Double.parseDouble(input);
            return v > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidAccountNumber(String acct) {
        return acct != null && acct.matches("^[0-9]{10}$");
    }

    /**
     * Sanitises a free-text field: trims whitespace and removes SQL/script
     * injection characters.
     */
    public static String sanitise(String input) {
        if (input == null) return "";
        return input.trim()
                    .replaceAll("[<>\"'%;()&+]", "")
                    .replaceAll("--", "");
    }

    /** Throws IllegalArgumentException with a human-readable message if invalid. */
    public static void requireValidEmail(String email) {
        if (!isValidEmail(email))
            throw new IllegalArgumentException("Invalid email format: " + email);
    }

    public static void requireValidPhone(String phone) {
        if (!isValidPhone(phone))
            throw new IllegalArgumentException("Invalid phone number: " + phone);
    }

    public static void requireValidPassword(String password) {
        if (!isValidPassword(password))
            throw new IllegalArgumentException(
                "Password must be >=8 chars with uppercase, lowercase, digit & special char.");
    }
}
