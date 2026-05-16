package com.banking.model;

/**
 * A system administrator with elevated privileges.
 */
public class Admin extends User {

    private final int adminLevel;   // 1 = standard, 2 = super-admin

    public Admin(String userId, String fullName, String email,
                 String phone, String password, int adminLevel) {
        super(userId, fullName, email, phone, password, Role.ADMIN);
        this.adminLevel = adminLevel;
    }

    public int getAdminLevel() { return adminLevel; }
}
