package com.banking.transaction;

import com.banking.account.AccountService;
import com.banking.exception.AccountException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.LimitExceededException;
import com.banking.model.*;
import com.banking.security.AuditLog;
import com.banking.security.SecurityService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Module 3 - Transaction Engine.
 *
 * Every transaction is atomic: either it fully completes or it rolls back
 * (via catch blocks that reverse any partial state change).
 *
 * Enforces per-account daily withdrawal limits tracked in a local map.
 */
public class TransactionEngine {

    private static final BigDecimal DEFAULT_DAILY_WITHDRAWAL_LIMIT = new BigDecimal("100000");
    private static final BigDecimal DEFAULT_DAILY_TRANSFER_LIMIT   = new BigDecimal("200000");
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD     = new BigDecimal("500000");

    private final AccountService accountService;
    private final AuditLog audit = AuditLog.getInstance();

    /** Tracks today's withdrawal total per account: acctNo -> amount */
    private final Map<String, BigDecimal> dailyWithdrawals = new HashMap<>();
    /** Tracks today's transfer total per account */
    private final Map<String, BigDecimal> dailyTransfers   = new HashMap<>();
    /** The date the daily counters were last reset */
    private LocalDate counterDate = LocalDate.now();

    public TransactionEngine(AccountService accountService) {
        this.accountService = accountService;
    }

    // -- Cash Deposit ----------------------------------------------------------

    /**
     * Deposits cash into an account.
     *
     * @return the completed Transaction
     */
    public Transaction deposit(String accountNumber, BigDecimal amount, String description)
            throws AccountException {
        resetDailyCountersIfNewDay();
        BankAccount acct = accountService.getAccount(accountNumber);

        acct.deposit(amount, description);

        Transaction txn = acct.getTransactions().get(acct.getTransactions().size() - 1);
        audit.logTransaction("SYSTEM", txn.getTransactionId(), accountNumber,
                "DEPOSIT PKR " + amount);

        if (amount.compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0)
            audit.flagTransaction("SYSTEM", "Large deposit PKR " + amount, accountNumber);

        txn.generateReceipt();
        return txn;
    }

    // -- Cash Withdrawal -------------------------------------------------------

    /**
     * Withdraws cash with a daily limit check.
     *
     * @throws LimitExceededException if the daily limit would be breached
     */
    public Transaction withdraw(String accountNumber, BigDecimal amount, String description)
            throws AccountException, InsufficientFundsException, LimitExceededException {
        resetDailyCountersIfNewDay();

        BigDecimal todaySoFar = dailyWithdrawals.getOrDefault(accountNumber, BigDecimal.ZERO);
        BigDecimal newTotal   = todaySoFar.add(amount);

        if (newTotal.compareTo(DEFAULT_DAILY_WITHDRAWAL_LIMIT) > 0)
            throw new LimitExceededException("withdrawal", newTotal, DEFAULT_DAILY_WITHDRAWAL_LIMIT);

        BankAccount acct = accountService.getAccount(accountNumber);
        acct.withdraw(amount, description);

        dailyWithdrawals.put(accountNumber, newTotal);

        Transaction txn = acct.getTransactions().get(acct.getTransactions().size() - 1);
        audit.logTransaction("SYSTEM", txn.getTransactionId(), accountNumber,
                "WITHDRAWAL PKR " + amount);
        txn.generateReceipt();
        return txn;
    }

    // -- Fund Transfer (Atomic) ------------------------------------------------

    /**
     * Atomically debits the source and credits the destination.
     * If the credit fails, the debit is reversed.
     */
    public void transfer(String fromAcct, String toAcct, BigDecimal amount, String description)
            throws AccountException, InsufficientFundsException, LimitExceededException {
        resetDailyCountersIfNewDay();

        if (fromAcct.equals(toAcct))
            throw new AccountException("Cannot transfer to the same account.");

        BigDecimal todaySoFar = dailyTransfers.getOrDefault(fromAcct, BigDecimal.ZERO);
        BigDecimal newTotal   = todaySoFar.add(amount);

        if (newTotal.compareTo(DEFAULT_DAILY_TRANSFER_LIMIT) > 0)
            throw new LimitExceededException("transfer", newTotal, DEFAULT_DAILY_TRANSFER_LIMIT);

        BankAccount source = accountService.getAccount(fromAcct);
        BankAccount dest   = accountService.getAccount(toAcct);

        // --- ATOMIC BLOCK START ---
        source.withdraw(amount, "Transfer to " + SecurityService.maskAccountNumber(toAcct));
        try {
            dest.deposit(amount, "Transfer from " + SecurityService.maskAccountNumber(fromAcct));
        } catch (AccountException e) {
            // Rollback: reverse the debit on source
            source.deposit(amount, "ROLLBACK: reversed transfer");
            throw new AccountException("Transfer failed (destination error). Debit reversed. " + e.getMessage());
        }
        // --- ATOMIC BLOCK END ---

        dailyTransfers.put(fromAcct, newTotal);

        // Record explicit TRANSFER_OUT / TRANSFER_IN transactions
        Transaction out = new Transaction(TransactionType.TRANSFER_OUT, amount,
                fromAcct, description + " -> " + SecurityService.maskAccountNumber(toAcct));
        Transaction in  = new Transaction(TransactionType.TRANSFER_IN, amount,
                toAcct, description + " <- " + SecurityService.maskAccountNumber(fromAcct));
        source.addTransaction(out);
        dest.addTransaction(in);

        audit.logTransaction("SYSTEM", out.getTransactionId(), fromAcct,
                "TRANSFER_OUT PKR " + amount + " to " + SecurityService.maskAccountNumber(toAcct));
        out.generateReceipt();
    }

    // -- Bill Payment ----------------------------------------------------------

    /**
     * Pays a bill by debiting the customer's account.
     */
    public Transaction payBill(String accountNumber, BigDecimal amount,
                               String billerName, String billReference)
            throws AccountException, InsufficientFundsException, LimitExceededException {
        String description = "Bill Payment: " + billerName + " Ref=" + billReference;
        return withdraw(accountNumber, amount, description);
    }

    // -- Transaction History ---------------------------------------------------

    /**
     * Returns the full transaction history of an account,
     * searchable by a keyword in the description.
     */
    public List<Transaction> getHistory(String accountNumber, String keyword)
            throws AccountException {
        List<Transaction> all = accountService.getAccount(accountNumber).getTransactions();
        if (keyword == null || keyword.isBlank()) return all;
        String lower = keyword.toLowerCase();
        return all.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(lower)
                          || t.getType().name().toLowerCase().contains(lower))
                .collect(java.util.stream.Collectors.toList());
    }

    /** Prints the mini-statement: last 5 transactions. */
    public void printMiniStatement(String accountNumber) throws AccountException {
        List<Transaction> txns = accountService.getAccount(accountNumber).getTransactions();
        System.out.println("\n-- Mini Statement: " + SecurityService.maskAccountNumber(accountNumber) + " --");
        int start = Math.max(0, txns.size() - 5);
        if (txns.isEmpty()) { System.out.println("  No transactions yet."); return; }
        txns.subList(start, txns.size()).forEach(System.out::println);
    }

    // -- Helpers ---------------------------------------------------------------

    /** Resets daily counters at midnight. */
    private void resetDailyCountersIfNewDay() {
        if (!LocalDate.now().equals(counterDate)) {
            dailyWithdrawals.clear();
            dailyTransfers.clear();
            counterDate = LocalDate.now();
        }
    }

    public BigDecimal getDailyWithdrawalsToday(String accountNumber) {
        return dailyWithdrawals.getOrDefault(accountNumber, BigDecimal.ZERO);
    }
}
