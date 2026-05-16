package com.banking.report;

import com.banking.account.AccountService;
import com.banking.loan.LoanService;
import com.banking.model.*;
import com.banking.security.AuditLog;
import com.banking.security.SecurityService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


// Reporting module
/**
 * Module 6 - Reporting & Statements.
 *
 * Provides:
 *  - Mini statement (last 5 transactions)
 *  - Full account statement (date-range filtered)
 *  - PDF export via Java File I/O (plaintext .txt simulating PDF)
 *  - Admin Dashboard Summary
 */
public class ReportService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    private final AccountService accountService;
    private final LoanService loanService;
    private final AuditLog audit = AuditLog.getInstance();

    public ReportService(AccountService accountService, LoanService loanService) {
        this.accountService = accountService;
        this.loanService    = loanService;
    }

    // -- Mini Statement --------------------------------------------------------

    /**
     * Prints the last 5 transactions for an account to the console.
     */
    public void printMiniStatement(String accountNumber) throws Exception {
        BankAccount acct = accountService.getAccount(accountNumber);
        List<Transaction> txns = acct.getTransactions();
        int start = Math.max(0, txns.size() - 5);

        System.out.println("\n+==================================================+");
        System.out.println("|                  MINI STATEMENT                 |");
        System.out.println("+==================================================+");
        System.out.printf( "|  Account : %-38s|%n", SecurityService.maskAccountNumber(accountNumber));
        System.out.printf( "|  Owner   : %-38s|%n", acct.getOwner().getFullName());
        System.out.printf( "|  Balance : PKR %-35.2f|%n", acct.getBalance());
        System.out.println("+==================================================+");
        if (txns.isEmpty()) {
            System.out.println("|  No transactions found.                          |");
        } else {
            txns.subList(start, txns.size()).forEach(t ->
                    System.out.printf("|  %-48s|%n",
                            t.toString().length() > 48 ? t.toString().substring(0, 45) + "..." : t));
        }
        System.out.println("+==================================================+");
    }

    // -- Full Account Statement ------------------------------------------------

    /**
     * Prints a full statement filtered by date range, chronologically sorted.
     *
     * @param accountNumber target account
     * @param from          start date (inclusive)
     * @param to            end date (inclusive)
     */
    public void printFullStatement(String accountNumber,
                                    LocalDate from, LocalDate to) throws Exception {
        BankAccount acct = accountService.getAccount(accountNumber);
        List<Transaction> filtered = acct.getTransactions().stream()
                .filter(t -> {
                    LocalDate txDate = t.getTimestamp().toLocalDate();
                    return !txDate.isBefore(from) && !txDate.isAfter(to);
                })
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());

        System.out.println("\n------------ ACCOUNT STATEMENT ------------");
        System.out.println("  Account  : " + SecurityService.maskAccountNumber(accountNumber));
        System.out.println("  Owner    : " + acct.getOwner().getFullName());
        System.out.println("  Type     : " + acct.getAccountType());
        System.out.println("  Period   : " + from.format(DATE_FMT) + " -> " + to.format(DATE_FMT));
        System.out.printf( "  Balance  : PKR %,.2f%n", acct.getBalance());
        System.out.println("---------------------------------------------");
        if (filtered.isEmpty()) {
            System.out.println("  No transactions in this period.");
        } else {
            filtered.forEach(System.out::println);
        }
        System.out.println("-------------------------------------------");
    }

    // -- PDF Export (File I/O) -------------------------------------------------

    /**
     * Exports the full statement to a .txt file (simulating PDF export).
     * The file is written using Java File I/O.
     *
     * @param accountNumber target account
     * @param from          start date
     * @param to            end date
     * @param filePath      output file path (e.g. "statement_12345.txt")
     */
    public void exportStatementToFile(String accountNumber,
                                       LocalDate from, LocalDate to,
                                       String filePath) throws Exception {
        BankAccount acct = accountService.getAccount(accountNumber);
        List<Transaction> filtered = acct.getTransactions().stream()
                .filter(t -> {
                    LocalDate txDate = t.getTimestamp().toLocalDate();
                    return !txDate.isBefore(from) && !txDate.isAfter(to);
                })
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("==============================================");
            pw.println("        SECURE BANK - ACCOUNT STATEMENT      ");
            pw.println("==============================================");
            pw.println("Generated : " + LocalDateTime.now().format(DATETIME_FMT));
            pw.println("Account   : " + SecurityService.maskAccountNumber(accountNumber));
            pw.println("Owner     : " + acct.getOwner().getFullName());
            pw.println("Type      : " + acct.getAccountType());
            pw.println("Period    : " + from.format(DATE_FMT) + " to " + to.format(DATE_FMT));
            pw.printf( "Balance   : PKR %,.2f%n", acct.getBalance());
            pw.println("----------------------------------------------");
            if (filtered.isEmpty()) {
                pw.println("  No transactions in this period.");
            } else {
                filtered.forEach(t -> pw.println(t.toString()));
            }
            pw.println("==============================================");
            pw.println("         END OF STATEMENT                    ");
            pw.println("==============================================");
        }

        audit.log(acct.getOwner().getUserId(), "EXPORT_STATEMENT", accountNumber);
        System.out.println("[Report] Statement exported to: " + filePath);
    }

    // -- Admin Dashboard -------------------------------------------------------

    /**
     * Prints an admin-level dashboard summary.
     */
    public void printAdminDashboard() {
        Map<String, BankAccount> allAccounts = accountService.getAllAccounts();
        Map<String, Loan> allLoans = loanService.getAllLoans();

        long totalAccounts   = allAccounts.size();
        long activeAccounts  = allAccounts.values().stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();
        long suspendedAccts  = allAccounts.values().stream()
                .filter(a -> a.getStatus() == AccountStatus.SUSPENDED).count();

        BigDecimal totalDeposits = allAccounts.values().stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingLoans = allLoans.values().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .map(Loan::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeLoans    = allLoans.values().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();
        long defaultedLoans = allLoans.values().stream()
                .filter(l -> l.getStatus() == LoanStatus.DEFAULTED).count();

        int flaggedTxns = audit.getFlaggedCount();

        System.out.println("\n+==================================================+");
        System.out.println("|            ADMIN DASHBOARD SUMMARY              |");
        System.out.println("+==================================================+");
        System.out.printf( "|  Total Accounts    : %-28d|%n", totalAccounts);
        System.out.printf( "|  Active Accounts   : %-28d|%n", activeAccounts);
        System.out.printf( "|  Suspended Accounts: %-28d|%n", suspendedAccts);
        System.out.println("+==================================================+");
        System.out.printf( "|  Total Deposits    : PKR %-25,.2f|%n", totalDeposits);
        System.out.printf( "|  Outstanding Loans : PKR %-25,.2f|%n", outstandingLoans);
        System.out.println("+==================================================+");
        System.out.printf( "|  Active Loans      : %-28d|%n", activeLoans);
        System.out.printf( "|  Defaulted Loans   : %-28d|%n", defaultedLoans);
        System.out.println("+==================================================+");
        System.out.printf( "|  Flagged Txns      : %-28d|%n", flaggedTxns);
        System.out.println("+==================================================+");
    }
}
