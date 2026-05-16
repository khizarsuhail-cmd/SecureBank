package com.banking.account;

import com.banking.exception.AccountException;
import com.banking.model.*;
import com.banking.security.AuditLog;
import com.banking.security.SecurityService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Account module

/**
 * Module 2 - Account Management.
 *
 * Opens accounts, manages status, applies interest,
 * and enforces closure rules.
 */
public class AccountService {

    /** In-memory account store: accountNumber -> BankAccount */
    private final Map<String, BankAccount> accounts = new HashMap<>();
    private final AuditLog audit = AuditLog.getInstance();
    private final Random rng     = new Random();

    // -- Account Opening -------------------------------------------------------

    /**
     * Opens a SAVINGS account and links it to the customer.
     */
    public SavingsAccount openSavingsAccount(Customer owner,
                                              BigDecimal initialDeposit,
                                              double annualRate) throws AccountException {
        validateInitialDeposit(initialDeposit, new BigDecimal("500"));
        String acctNo = generateAccountNumber();
        SavingsAccount acct = new SavingsAccount(acctNo, owner, initialDeposit, annualRate);
        accounts.put(acctNo, acct);
        owner.openAccount(acct);
        audit.log(owner.getUserId(), "OPEN_SAVINGS", acctNo);
        System.out.printf("[Account] Savings account opened: %s for %s%n",
                acctNo, owner.getFullName());
        return acct;
    }

    /**
     * Opens a CURRENT account with overdraft facility.
     */
    public CurrentAccount openCurrentAccount(Customer owner,
                                              BigDecimal initialDeposit,
                                              BigDecimal overdraftLimit,
                                              BigDecimal dailyLimit) throws AccountException {
        validateInitialDeposit(initialDeposit, new BigDecimal("1000"));
        String acctNo = generateAccountNumber();
        CurrentAccount acct = new CurrentAccount(acctNo, owner, initialDeposit,
                overdraftLimit, dailyLimit);
        accounts.put(acctNo, acct);
        owner.openAccount(acct);
        audit.log(owner.getUserId(), "OPEN_CURRENT", acctNo);
        System.out.printf("[Account] Current account opened: %s for %s%n",
                acctNo, owner.getFullName());
        return acct;
    }

    /**
     * Opens a FIXED DEPOSIT account.
     */
    public FixedDepositAccount openFixedDeposit(Customer owner,
                                                 BigDecimal principal,
                                                 double fixedRate,
                                                 int tenureMonths) throws AccountException {
        validateInitialDeposit(principal, new BigDecimal("5000"));
        String acctNo = generateAccountNumber();
        FixedDepositAccount acct = new FixedDepositAccount(acctNo, owner,
                principal, fixedRate, tenureMonths);
        accounts.put(acctNo, acct);
        owner.openAccount(acct);
        audit.log(owner.getUserId(), "OPEN_FD", acctNo);
        System.out.printf("[Account] Fixed Deposit opened: %s | Matures: %s%n",
                acctNo, acct.getMaturityDate());
        return acct;
    }

    // -- Status Management -----------------------------------------------------

    public void suspendAccount(String accountNumber, String reason) throws AccountException {
        BankAccount acct = getAccount(accountNumber);
        acct.setStatus(AccountStatus.SUSPENDED);
        audit.log("SYSTEM", "SUSPEND_ACCOUNT reason=" + reason,
                SecurityService.maskAccountNumber(accountNumber));
        System.out.println("[Account] Account suspended: " + SecurityService.maskAccountNumber(accountNumber));
    }

    public void reactivateAccount(String accountNumber) throws AccountException {
        BankAccount acct = getAccount(accountNumber);
        if (acct.getStatus() == AccountStatus.CLOSED)
            throw new AccountException("Closed accounts cannot be reactivated.");
        acct.setStatus(AccountStatus.ACTIVE);
        audit.log("SYSTEM", "REACTIVATE_ACCOUNT", accountNumber);
    }

    /**
     * Closes an account - only allowed if balance is zero or refunded.
     */
    public void closeAccount(Customer owner, String accountNumber) throws AccountException {
        BankAccount acct = getAccount(accountNumber);
        if (!acct.getOwner().getUserId().equals(owner.getUserId()))
            throw new AccountException("Account does not belong to this customer.");
        if (acct.getBalance().compareTo(BigDecimal.ZERO) > 0)
            throw new AccountException(
                "Account has remaining balance of PKR " + acct.getBalance()
                + ". Please withdraw before closing.");
        acct.setStatus(AccountStatus.CLOSED);
        owner.removeAccount(acct);
        audit.log(owner.getUserId(), "CLOSE_ACCOUNT", accountNumber);
        System.out.println("[Account] Account closed: " + SecurityService.maskAccountNumber(accountNumber));
    }

    // -- Balance / Inquiry -----------------------------------------------------

    public BigDecimal getBalance(String accountNumber) throws AccountException {
        return getAccount(accountNumber).getBalance();
    }

    public void printBalanceInquiry(String accountNumber) throws AccountException {
        BankAccount acct = getAccount(accountNumber);
        System.out.println("\n-- Balance Inquiry --");
        System.out.println("  Account : " + SecurityService.maskAccountNumber(accountNumber));
        System.out.println("  Type    : " + acct.getAccountType());
        System.out.println("  Status  : " + acct.getStatus());
        System.out.printf( "  Balance : PKR %,.2f%n", acct.getBalance());
    }

    // -- Interest Calculation --------------------------------------------------

    /** Applies monthly interest to every active savings account. */
    public void applyMonthlyInterestAll() {
        accounts.values().stream()
                .filter(a -> a instanceof SavingsAccount)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .map(a -> (SavingsAccount) a)
                .forEach(SavingsAccount::applyMonthlyInterest);
    }

    // -- Lookup ----------------------------------------------------------------

    public BankAccount getAccount(String accountNumber) throws AccountException {
        BankAccount acct = accounts.get(accountNumber);
        if (acct == null)
            throw new AccountException("Account not found: " + accountNumber);
        return acct;
    }

    public Map<String, BankAccount> getAllAccounts() { return new HashMap<>(accounts); }

    // -- Helpers ---------------------------------------------------------------

    /** Generates a unique 10-digit account number. */
    private String generateAccountNumber() {
        String num;
        do {
            num = String.format("%010d", (long)(rng.nextDouble() * 9_000_000_000L) + 1_000_000_000L);
        } while (accounts.containsKey(num));
        return num;
    }

    private void validateInitialDeposit(BigDecimal deposit, BigDecimal minimum)
            throws AccountException {
        if (deposit == null || deposit.compareTo(minimum) < 0)
            throw new AccountException(
                "Minimum initial deposit is PKR " + minimum);
    }
}
