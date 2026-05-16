package com.banking.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A bank customer who owns one or more accounts and may have loans.
 */
public class Customer extends User {

    private final String nationalId;
    private final LocalDate dateOfBirth;
    private final List<BankAccount> accounts;
    private final List<Loan> loans;

    public Customer(String userId, String fullName, String email,
                    String phone, String password,
                    String nationalId, LocalDate dateOfBirth) {
        super(userId, fullName, email, phone, password, Role.CUSTOMER);
        this.nationalId   = nationalId;
        this.dateOfBirth  = dateOfBirth;
        this.accounts     = new ArrayList<>();
        this.loans        = new ArrayList<>();
    }

    /** Links a newly created bank account to this customer. */
    public void openAccount(BankAccount account) {
        accounts.add(account);
    }

    /** Prints a summary of all owned accounts. */
    public void viewStatement() {
        System.out.println("\n-- Accounts for " + getFullName() + " --");
        if (accounts.isEmpty()) {
            System.out.println("  No accounts found.");
        } else {
            accounts.forEach(a -> System.out.println("  " + a));
        }
    }

    public String        getNationalId()  { return nationalId; }
    public LocalDate     getDateOfBirth() { return dateOfBirth; }
    public List<BankAccount> getAccounts(){ return new ArrayList<>(accounts); }
    public List<Loan>    getLoans()       { return new ArrayList<>(loans); }

    public void addLoan(Loan loan)        { loans.add(loan); }
    public void removeAccount(BankAccount a) { accounts.remove(a); }
}
