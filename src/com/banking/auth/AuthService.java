package com.banking.auth;

import com.banking.exception.AuthException;
import com.banking.model.*;
import com.banking.security.AuditLog;
import com.banking.security.InputValidator;
import com.banking.security.SecurityService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Module 1 - User Authentication & Identity Management.
 *
 * Handles registration, login, logout, lockout,
 * password reset, and role-based session tracking.
 */
public class AuthService {

    /** In-memory user store: userId -> User */
    private final Map<String, User> users = new HashMap<>();

    /** Active sessions: sessionToken -> User */
    private final Map<String, User> sessions = new HashMap<>();

    private final AuditLog audit = AuditLog.getInstance();

    // -- Registration ----------------------------------------------------------

    /**
     * Registers a new Customer after full input validation.
     *
     * @return the newly created Customer
     * @throws AuthException if any field fails validation or email already exists
     */
    public Customer registerCustomer(String fullName, String email, String phone,
                                     String dateOfBirthStr, String nationalId,
                                     String password) throws AuthException {
        // Validate every field before touching business logic
        if (!InputValidator.isValidName(fullName))
            throw new AuthException("Invalid name format.");
        if (!InputValidator.isValidEmail(email))
            throw new AuthException("Invalid email address.");
        if (!InputValidator.isValidPhone(phone))
            throw new AuthException("Invalid phone number (10-13 digits).");
        if (!InputValidator.isValidNationalId(nationalId))
            throw new AuthException("National ID must be 13 digits (CNIC format).");
        if (!InputValidator.isValidPassword(password))
            throw new AuthException(
                "Weak password. Use >=8 chars, uppercase, lowercase, digit & special char.");

        // Duplicate email check
        boolean emailTaken = users.values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        if (emailTaken) throw new AuthException("Email already registered.");

        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirthStr);   // expects YYYY-MM-DD
        } catch (Exception e) {
            throw new AuthException("Date of birth must be in YYYY-MM-DD format.");
        }

        String userId   = "C" + UUID.randomUUID().toString().replace("-","").substring(0, 8).toUpperCase();
        Customer customer = new Customer(userId, InputValidator.sanitise(fullName),
                email, phone, password, nationalId, dob);
        users.put(userId, customer);

        audit.logSecurity(userId, "CUSTOMER_REGISTERED email=" + SecurityService.maskEmail(email));
        System.out.println("[Auth] Customer registered successfully. ID: " + userId);
        return customer;
    }

    /**
     * Registers a Teller (Admin-only operation in a real system).
     */
    public Teller registerTeller(String fullName, String email, String phone,
                                 String password, String branchCode) throws AuthException {
        if (!InputValidator.isValidEmail(email))
            throw new AuthException("Invalid email.");
        if (!InputValidator.isValidPassword(password))
            throw new AuthException("Weak password.");

        String userId = "T" + UUID.randomUUID().toString().replace("-","").substring(0, 8).toUpperCase();
        String empId  = "EMP-" + (1000 + users.size());
        Teller teller = new Teller(userId, fullName, email, phone, password, empId, branchCode);
        users.put(userId, teller);
        audit.logSecurity(userId, "TELLER_REGISTERED empId=" + empId);
        return teller;
    }

    /**
     * Registers an Admin.
     */
    public Admin registerAdmin(String fullName, String email, String phone,
                               String password, int adminLevel) throws AuthException {
        if (!InputValidator.isValidEmail(email))
            throw new AuthException("Invalid email.");
        if (!InputValidator.isValidPassword(password))
            throw new AuthException("Weak password.");

        String userId = "A" + UUID.randomUUID().toString().replace("-","").substring(0, 8).toUpperCase();
        Admin admin = new Admin(userId, fullName, email, phone, password, adminLevel);
        users.put(userId, admin);
        audit.logSecurity(userId, "ADMIN_REGISTERED level=" + adminLevel);
        return admin;
    }

    // -- Login / Logout --------------------------------------------------------

    /**
     * Authenticates a user by email + password.
     *
     * @return a session token on success
     * @throws AuthException on bad credentials, locked account, or unknown user
     */
    public String login(String email, String password) throws AuthException {
        User user = users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);

        if (user == null)
            throw new AuthException("No account found for email: " + email);

        if (user.isLocked()) {
            audit.logSecurity(user.getUserId(), "LOGIN_ATTEMPT_LOCKED");
            throw new AuthException("Account locked after 5 failed attempts. Contact support.");
        }

        if (!user.login(password)) {
            int attempts = user.getFailedLoginAttempts();
            audit.logSecurity(user.getUserId(),
                    "FAILED_LOGIN attempt=" + attempts);
            if (user.isLocked()) {
                audit.logSecurity(user.getUserId(), "ACCOUNT_LOCKED");
                throw new AuthException("Too many failed attempts - account is now locked.");
            }
            throw new AuthException("Incorrect password. Attempts remaining: " + (5 - attempts));
        }

        // Issue session token
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        audit.logSecurity(user.getUserId(), "LOGIN_SUCCESS role=" + user.getRole());
        System.out.println("[Auth] Login successful. Welcome, " + user.getFullName() + " [" + user.getRole() + "]");
        return token;
    }

    /** Invalidates a session token. */
    public void logout(String sessionToken) {
        User user = sessions.remove(sessionToken);
        if (user != null) {
            audit.logSecurity(user.getUserId(), "LOGOUT");
            System.out.println("[Auth] " + user.getFullName() + " logged out.");
        }
    }

    // -- Password Reset --------------------------------------------------------

    /**
     * Resets a user's password after verifying their email.
     * In production this would send a reset link; here we simulate it.
     */
    public void resetPassword(String email, String newPassword) throws AuthException {
        if (!InputValidator.isValidPassword(newPassword))
            throw new AuthException("New password does not meet complexity requirements.");

        User user = users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new AuthException("Email not found."));

        user.resetPassword(newPassword);
        audit.logSecurity(user.getUserId(), "PASSWORD_RESET");
        // Simulate email notification
        System.out.println("[Auth] Password reset successful for " + SecurityService.maskEmail(email)
                + " (email notification simulated).");
    }

    // -- Admin Unlock ----------------------------------------------------------

    /** Allows an admin to manually unlock a locked account. */
    public void unlockAccount(String adminToken, String targetEmail) throws AuthException {
        User admin = getSessionUser(adminToken);
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.TELLER)
            throw new AuthException("Insufficient privileges.");

        User target = users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(targetEmail))
                .findFirst()
                .orElseThrow(() -> new AuthException("Target user not found."));

        target.unlockAccount();
        audit.logSecurity(target.getUserId(), "ACCOUNT_UNLOCKED_BY=" + admin.getUserId());
        System.out.println("[Auth] Account unlocked for " + target.getFullName());
    }

    // -- Session Helpers -------------------------------------------------------

    /**
     * Retrieves the User for an active session token.
     * @throws AuthException if token is invalid or expired
     */
    public User getSessionUser(String token) throws AuthException {
        User u = sessions.get(token);
        if (u == null) throw new AuthException("Invalid or expired session. Please log in again.");
        return u;
    }

    public boolean isLoggedIn(String token) { return sessions.containsKey(token); }

    /** Convenience: returns the customer from a session, or throws if wrong role. */
    public Customer getSessionCustomer(String token) throws AuthException {
        User u = getSessionUser(token);
        if (!(u instanceof Customer))
            throw new AuthException("This operation requires a CUSTOMER session.");
        return (Customer) u;
    }

    // -- User Lookup -----------------------------------------------------------

    public User findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    public Map<String, User> getAllUsers() { return new HashMap<>(users); }
}
