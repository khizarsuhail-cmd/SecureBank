package com.banking.model;

import com.banking.exception.AccountException;
import com.banking.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all bank account types.
 * Uses BigDecimal for all monetary calculations to avoid floating-point errors.
 */
public abstract class BankAccount {

    private final String accountNumber;   // 10-digit unique
    protected BigDecimal balance;
    private AccountStatus status;
    private final Customer owner;
    private final List<Transaction> transactions;
    private final LocalDateTime openedDate;
    private final AccountType accountType;

    protected BankAccount(String accountNumber, Customer owner,
                          BigDecimal initialDeposit, AccountType accountType) {
        this.accountNumber = accountNumber;
        this.owner         = owner;
        this.balance       = initialDeposit.setScale(2, RoundingMode.HALF_UP);
        this.status        = AccountStatus.ACTIVE;
        this.transactions  = new ArrayList<>();
        this.openedDate    = LocalDateTime.now();
        this.accountType   = accountType;
    }

    // -- Core Operations ------------------------------------------------------

    /**
     * Credits the account by the given amount and records a transaction.
     */
    public synchronized void deposit(BigDecimal amount, String description)
            throws AccountException {
        validateActive();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new AccountException("Deposit amount must be positive.");
        balance = balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        Transaction txn = new Transaction(TransactionType.DEPOSIT, amount, accountNumber, description);
        transactions.add(txn);
    }

    /**
     * Debits the account by the given amount and records a transaction.
     * Subclasses may override to allow overdraft.
     */
    public synchronized void withdraw(BigDecimal amount, String description)
            throws AccountException, InsufficientFundsException {
        validateActive();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new AccountException("Withdrawal amount must be positive.");
        if (balance.compareTo(amount) < 0)
            throw new InsufficientFundsException(balance, amount);
        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        Transaction txn = new Transaction(TransactionType.WITHDRAWAL, amount, accountNumber, description);
        transactions.add(txn);
    }

    /** Records an arbitrary transaction object directly (used by TransactionEngine). */
    public synchronized void addTransaction(Transaction txn) {
        transactions.add(txn);
    }

    /** Template method - each account type defines its own interest logic. */
    public abstract void calculateInterest();

    // -- Helpers --------------------------------------------------------------

    protected void validateActive() throws AccountException {
        if (status != AccountStatus.ACTIVE)
            throw new AccountException("Account " + accountNumber + " is " + status + ".");
    }

    // -- Getters / Setters -----------------------------------------------------

    public String        getAccountNumber() { return accountNumber; }
    public BigDecimal    getBalance()       { return balance; }
    public AccountStatus getStatus()        { return status; }
    public Customer      getOwner()         { return owner; }
    public AccountType   getAccountType()   { return accountType; }
    public LocalDateTime getOpenedDate()    { return openedDate; }

    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }

    public void setStatus(AccountStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Account[%s | %s | %s | Balance: PKR %,.2f]",
                accountNumber, accountType, status, balance);
    }
}
