package com.banking.model;

import com.banking.exception.AccountException;
import com.banking.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Savings account with monthly compound interest and minimum balance enforcement.
 */
public class SavingsAccount extends BankAccount {

    private double interestRate;          // annual rate, e.g. 0.06 for 6 %
    private final BigDecimal minBalance;

    public SavingsAccount(String accountNumber, Customer owner,
                          BigDecimal initialDeposit, double annualInterestRate) {
        super(accountNumber, owner, initialDeposit, AccountType.SAVINGS);
        this.interestRate = annualInterestRate;
        this.minBalance   = new BigDecimal("500.00");
    }

    /**
     * Applies monthly compound interest: balance x (1 + r/12).
     */
    @Override
    public void calculateInterest() {
        double monthlyRate = interestRate / 12.0;
        BigDecimal factor  = BigDecimal.valueOf(1 + monthlyRate);
        BigDecimal interest = balance.multiply(factor).subtract(balance)
                .setScale(2, RoundingMode.HALF_UP);
        balance = balance.add(interest).setScale(2, RoundingMode.HALF_UP);

        Transaction txn = new Transaction(TransactionType.INTEREST_CREDIT,
                interest, getAccountNumber(), "Monthly interest credit");
        addTransaction(txn);
        System.out.printf("[Interest] PKR %.2f credited to account %s%n", interest, getAccountNumber());
    }

    /** Enforces minimum balance on withdrawals. */
    @Override
    public synchronized void withdraw(BigDecimal amount, String description)
            throws AccountException, InsufficientFundsException {
        if (balance.subtract(amount).compareTo(minBalance) < 0)
            throw new AccountException(
                    "Withdrawal would breach minimum balance of PKR " + minBalance);
        super.withdraw(amount, description);
    }

    public void applyMonthlyInterest() { calculateInterest(); }

    public double      getInterestRate() { return interestRate; }
    public BigDecimal  getMinBalance()   { return minBalance; }
    public void        setInterestRate(double r) { this.interestRate = r; }
}
