package com.banking.model;

import com.banking.exception.AccountException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Fixed deposit account - locked until maturity date.
 */
public class FixedDepositAccount extends BankAccount {

    private final LocalDate maturityDate;
    private final double fixedRate;        // annual rate

    public FixedDepositAccount(String accountNumber, Customer owner,
                                BigDecimal principal, double fixedRate,
                                int tenureMonths) {
        super(accountNumber, owner, principal, AccountType.FIXED_DEPOSIT);
        this.fixedRate    = fixedRate;
        this.maturityDate = LocalDate.now().plusMonths(tenureMonths);
    }

    /**
     * Calculates simple interest accrued at maturity.
     * A proper FD would compound, but for reporting we show accrued interest.
     */
    @Override
    public void calculateInterest() {
        BigDecimal interest = balance
                .multiply(BigDecimal.valueOf(fixedRate))
                .setScale(2, RoundingMode.HALF_UP);
        System.out.printf("[FD Interest] Accrued PKR %.2f for account %s (matures %s)%n",
                interest, getAccountNumber(), maturityDate);
    }

    /**
     * Withdrawal blocked before maturity.
     */
    @Override
    public synchronized void withdraw(BigDecimal amount, String description)
            throws AccountException {
        if (LocalDate.now().isBefore(maturityDate))
            throw new AccountException(
                    "Fixed deposit cannot be withdrawn before maturity date: " + maturityDate);
        try {
            super.withdraw(amount, description);
        } catch (Exception e) {
            throw new AccountException(e.getMessage());
        }
    }

    /** Returns true if the FD has reached or passed its maturity date. */
    public boolean checkMaturity() {
        return !LocalDate.now().isBefore(maturityDate);
    }

    public LocalDate getMaturityDate() { return maturityDate; }
    public double    getFixedRate()    { return fixedRate; }
}
