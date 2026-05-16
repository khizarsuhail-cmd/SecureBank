package com.banking.model;

import com.banking.security.SecurityService;

/**
 * Abstract base for all system users.
 * Password is never stored in plain text - only the BCrypt hash is kept.
 */
public abstract class User {

    private final String userId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private Role role;
    private int failedLoginAttempts;
    private boolean locked;

    protected User(String userId, String fullName, String email,
                   String phone, String plainPassword, Role role) {
        this.userId    = userId;
        this.fullName  = fullName;
        this.email     = email;
        this.phone     = phone;
        this.passwordHash = SecurityService.hashPassword(plainPassword);
        this.role      = role;
        this.failedLoginAttempts = 0;
        this.locked    = false;
    }

    // -- Authentication --------------------------------------------------------

    /**
     * Verifies a plain-text password against the stored hash.
     * Increments fail counter; locks account after 5 failures.
     *
     * @return true if credentials are valid and account is not locked
     */
    public boolean login(String plainPassword) {
        if (locked) return false;
        if (SecurityService.verifyPassword(plainPassword, passwordHash)) {
            failedLoginAttempts = 0;
            return true;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= 5) locked = true;
        return false;
    }

    /** Resets the password to a new value (hashes internally). */
    public void resetPassword(String newPlainPassword) {
        this.passwordHash = SecurityService.hashPassword(newPlainPassword);
        this.failedLoginAttempts = 0;
        this.locked = false;
    }

    public void unlockAccount() {
        this.locked = false;
        this.failedLoginAttempts = 0;
    }

    // -- Getters / Setters -----------------------------------------------------

    public String  getUserId()             { return userId; }
    public String  getFullName()           { return fullName; }
    public String  getEmail()              { return email; }
    public String  getPhone()              { return phone; }
    public Role    getRole()               { return role; }
    public boolean isLocked()              { return locked; }
    public int     getFailedLoginAttempts(){ return failedLoginAttempts; }
    public String  getPasswordHash()       { return passwordHash; }

    public void setFullName(String n) { this.fullName = n; }
    public void setEmail(String e)    { this.email = e; }
    public void setPhone(String p)    { this.phone = p; }
    public void setLocked(boolean l)  { this.locked = l; }

    @Override
    public String toString() {
        return String.format("User[%s | %s | %s | %s | locked=%b]",
                userId, fullName, email, role, locked);
    }
}
