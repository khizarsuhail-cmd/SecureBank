package com.banking.model;

/**
 * A bank teller who can perform cash operations on behalf of customers.
 */
public class Teller extends User {

    private final String employeeId;
    private final String branchCode;

    public Teller(String userId, String fullName, String email,
                  String phone, String password,
                  String employeeId, String branchCode) {
        super(userId, fullName, email, phone, password, Role.TELLER);
        this.employeeId = employeeId;
        this.branchCode = branchCode;
    }

    public String getEmployeeId() { return employeeId; }
    public String getBranchCode() { return branchCode; }
}
