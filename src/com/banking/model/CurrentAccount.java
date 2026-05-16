package com.banking.model;

import com.banking.exception.AccountException;
import com.banking.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Current (chequing) account with overdraft facility and daily transaction limit.
 */
public class CurrentAccount extends BankAccount {

    private BigDecimal overdraftLimit;
    private final BigDecimal dailyLimit;

    public CurrentAccount(String accountNumber, Customer owner,
                          BigDecimal initialDeposit,
                          BigDecimal overdraftLimit, BigDecimal dailyLimit) {
        super(accountNumber, owner, initialDeposit, AccountType.CURRENT);
        this.overdraftLimit = overdraftLimit;
        this.dailyLimit     = dailyLimit;
    }

    /** No interest on current accounts - no-op implementation. */
    @Override
    public void calculateInterest() {
        // Current accounts typically do not earn interest.
    }

    /**
     * Allows withdrawal up to (balance + overdraftLimit).
     */
    @Override
    public synchronized void withdraw(BigDecimal amount, String description)
            throws AccountException, InsufficientFundsException {
        validateActive();
        BigDecimal available = balance.add(overdraftLimit);
        if (amount.compareTo(available) > 0)
            throw new InsufficientFundsException(available, amount);
        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        Transaction txn = new Transaction(TransactionType.WITHDRAWAL, amount,
                getAccountNumber(), description);
        addTransaction(txn);
    }

    /** Returns true if the account is currently in overdraft. */
    public boolean checkOverdraft() {
        return balance.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public BigDecimal getDailyLimit()     { return dailyLimit; }
    public void setOverdraftLimit(BigDecimal l) { this.overdraftLimit = l; }
}
