package com.banking.app;

import com.banking.account.AccountService;
import com.banking.auth.AuthService;
import com.banking.exception.*;
import com.banking.loan.EMICalculator;
import com.banking.loan.LoanService;
import com.banking.model.*;
import com.banking.report.ReportService;
import com.banking.security.AuditLog;
import com.banking.security.InputValidator;
import com.banking.transaction.TransactionEngine;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Entry point for the Secure Financial Services Management System.
 *
 * Console-based application wiring together all 6 modules.
 * Email / SMS notifications are simulated via console output.
 */
public class MainApplication {

    // -- Services --------------------------------------------------------------
    private static final AuthService       authService    = new AuthService();
    private static final AccountService    accountService = new AccountService();
    private static final TransactionEngine txnEngine      = new TransactionEngine(accountService);
    private static final LoanService       loanService    = new LoanService(accountService);
    private static final ReportService     reportService  = new ReportService(accountService, loanService);

    private static final Scanner sc = new Scanner(System.in);

    // Active session
    private static String sessionToken = null;
    private static User   currentUser  = null;

    // -- Main ------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        // Force UTF-8 output so all characters render correctly in any terminal
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        printBanner();
        seedDemoData();     // pre-load one admin for convenience

        boolean running = true;
        while (running) {
            if (sessionToken == null) {
                running = showGuestMenu();
            } else {
                switch (currentUser.getRole()) {
                    case CUSTOMER -> showCustomerMenu();
                    case TELLER   -> showTellerMenu();
                    case ADMIN    -> showAdminMenu();
                }
            }
        }
        System.out.println("\nGoodbye. Thank you for banking with SecureBank.");
        sc.close();
    }

    // -- Guest Menu (unauthenticated) ------------------------------------------

    private static boolean showGuestMenu() {
        System.out.println("\n========== SecureBank ==========");
        System.out.println("  1. Login");
        System.out.println("  2. Register (New Customer)");
        System.out.println("  3. Reset Password");
        System.out.println("  0. Exit");
        System.out.print("Choice: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1" -> handleLogin();
            case "2" -> handleRegister();
            case "3" -> handlePasswordReset();
            case "0" -> { return false; }
            default  -> System.out.println("Invalid option.");
        }
        return true;
    }

    // -- Customer Menu ---------------------------------------------------------

    private static void showCustomerMenu() {
        Customer cust = (Customer) currentUser;
        System.out.println("\n====== Customer Menu [" + cust.getFullName() + "] ======");
        System.out.println("  1.  Open Savings Account");
        System.out.println("  2.  Open Current Account");
        System.out.println("  3.  Open Fixed Deposit");
        System.out.println("  4.  Balance Inquiry");
        System.out.println("  5.  Deposit Cash");
        System.out.println("  6.  Withdraw Cash");
        System.out.println("  7.  Fund Transfer");
        System.out.println("  8.  Pay Bill");
        System.out.println("  9.  Transaction History");
        System.out.println("  10. Mini Statement");
        System.out.println("  11. Full Statement (date range)");
        System.out.println("  12. Export Statement to File");
        System.out.println("  13. Apply for Loan");
        System.out.println("  14. View My Loans");
        System.out.println("  15. Make Loan Repayment");
        System.out.println("  16. EMI Calculator");
        System.out.println("  0.  Logout");
        System.out.print("Choice: ");
        String choice = sc.nextLine().trim();

        try {
            switch (choice) {
                case "1"  -> openSavingsAccount(cust);
                case "2"  -> openCurrentAccount(cust);
                case "3"  -> openFixedDeposit(cust);
                case "4"  -> balanceInquiry();
                case "5"  -> depositCash();
                case "6"  -> withdrawCash();
                case "7"  -> fundTransfer();
                case "8"  -> payBill();
                case "9"  -> transactionHistory();
                case "10" -> miniStatement();
                case "11" -> fullStatement();
                case "12" -> exportStatement();
                case "13" -> applyLoan(cust);
                case "14" -> viewLoans(cust);
                case "15" -> makeLoanRepayment();
                case "16" -> emiCalculator();
                case "0"  -> logout();
                default   -> System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    // -- Teller Menu -----------------------------------------------------------

    private static void showTellerMenu() {
        System.out.println("\n====== Teller Menu ======");
        System.out.println("  1. Deposit to Customer Account");
        System.out.println("  2. Withdraw from Customer Account");
        System.out.println("  3. Balance Inquiry");
        System.out.println("  4. Mini Statement");
        System.out.println("  5. Unlock Customer Account");
        System.out.println("  0. Logout");
        System.out.print("Choice: ");
        String choice = sc.nextLine().trim();

        try {
            switch (choice) {
                case "1" -> depositCash();
                case "2" -> withdrawCash();
                case "3" -> balanceInquiry();
                case "4" -> miniStatement();
                case "5" -> unlockUserAccount();
                case "0" -> logout();
                default  -> System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    // -- Admin Menu ------------------------------------------------------------

    private static void showAdminMenu() {
        System.out.println("\n====== Admin Menu ======");
        System.out.println("  1. Dashboard Summary");
        System.out.println("  2. Register Teller");
        System.out.println("  3. Suspend Account");
        System.out.println("  4. Reactivate Account");
        System.out.println("  5. Apply Monthly Interest (All Savings)");
        System.out.println("  6. Detect Overdue Loans");
        System.out.println("  7. View Audit Log");
        System.out.println("  8. Unlock User Account");
        System.out.println("  0. Logout");
        System.out.print("Choice: ");
        String choice = sc.nextLine().trim();

        try {
            switch (choice) {
                case "1" -> reportService.printAdminDashboard();
                case "2" -> registerTeller();
                case "3" -> suspendAccount();
                case "4" -> reactivateAccount();
                case "5" -> { accountService.applyMonthlyInterestAll();
                              System.out.println("[Admin] Monthly interest applied."); }
                case "6" -> loanService.detectOverdue();
                case "7" -> AuditLog.getInstance().printAll();
                case "8" -> unlockUserAccount();
                case "0" -> logout();
                default  -> System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("[Error] " + e.getMessage());
        }
    }

    // ==========================================================================
    //  Handler methods
    // ==========================================================================

    private static void handleLogin() {
        System.out.print("Email   : "); String email    = sc.nextLine().trim();
        System.out.print("Password: "); String password = sc.nextLine().trim();
        try {
            sessionToken = authService.login(email, password);
            currentUser  = authService.getSessionUser(sessionToken);
        } catch (AuthException e) {
            System.out.println("[Login Failed] " + e.getMessage());
        }
    }

    private static void handleRegister() {
        System.out.println("\n+--------------------------------------------------+");
        System.out.println("|           NEW CUSTOMER REGISTRATION              |");
        System.out.println("+--------------------------------------------------+");

        System.out.println("  Full Name     [letters and spaces only, 2-60 chars]");
        System.out.print("  > "); String name = sc.nextLine().trim();

        System.out.println("  Email         [e.g. john@example.com]");
        System.out.print("  > "); String email = sc.nextLine().trim();

        System.out.println("  Phone         [10-13 digits, may start with +]");
        System.out.print("  > "); String phone = sc.nextLine().trim();

        System.out.println("  Date of Birth [format: YYYY-MM-DD, e.g. 1995-06-15]");
        System.out.print("  > "); String dob = sc.nextLine().trim();

        System.out.println("  National ID   [13-digit CNIC, digits only, no dashes]");
        System.out.print("  > "); String nid = sc.nextLine().trim();

        System.out.println("  Password      [min 8 chars, must include: uppercase,");
        System.out.println("                 lowercase, digit, special char e.g. Admin@1234]");
        System.out.print("  > "); String pass = sc.nextLine().trim();

        System.out.println("+--------------------------------------------------+");
        try {
            authService.registerCustomer(name, email, phone, dob, nid, pass);
            System.out.println("[Register] Registration successful! You may now log in.");
        } catch (AuthException e) {
            System.out.println("[Register Failed] " + e.getMessage());
        }
    }

    private static void handlePasswordReset() {
        System.out.print("Email       : "); String email = sc.nextLine().trim();
        System.out.print("New Password: "); String pass  = sc.nextLine().trim();
        try {
            authService.resetPassword(email, pass);
        } catch (AuthException e) {
            System.out.println("[Reset Failed] " + e.getMessage());
        }
    }

    private static void logout() throws AuthException {
        authService.logout(sessionToken);
        sessionToken = null;
        currentUser  = null;
    }

    // -- Account operations ----------------------------------------------------

    private static void openSavingsAccount(Customer cust) throws AccountException {
        System.out.print("Initial Deposit (min 500): "); BigDecimal dep = readAmount();
        accountService.openSavingsAccount(cust, dep, 0.06);   // 6 % p.a.
    }

    private static void openCurrentAccount(Customer cust) throws AccountException {
        System.out.print("Initial Deposit (min 1000): "); BigDecimal dep = readAmount();
        accountService.openCurrentAccount(cust, dep,
                new BigDecimal("10000"), new BigDecimal("100000"));
    }

    private static void openFixedDeposit(Customer cust) throws AccountException {
        System.out.print("Principal (min 5000): "); BigDecimal p = readAmount();
        System.out.print("Tenure (months)     : "); int months = Integer.parseInt(sc.nextLine().trim());
        accountService.openFixedDeposit(cust, p, 0.10, months);   // 10 % p.a.
    }

    private static void balanceInquiry() throws AccountException {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        accountService.printBalanceInquiry(acct);
    }

    private static void depositCash() throws AccountException {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        System.out.print("Amount        : "); BigDecimal amt = readAmount();
        System.out.print("Description   : "); String desc = InputValidator.sanitise(sc.nextLine());
        txnEngine.deposit(acct, amt, desc.isBlank() ? "Cash Deposit" : desc);
    }

    private static void withdrawCash() throws Exception {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        System.out.print("Amount        : "); BigDecimal amt = readAmount();
        System.out.print("Description   : "); String desc = InputValidator.sanitise(sc.nextLine());
        txnEngine.withdraw(acct, amt, desc.isBlank() ? "Cash Withdrawal" : desc);
    }

    private static void fundTransfer() throws Exception {
        System.out.print("From Account: "); String from = sc.nextLine().trim();
        System.out.print("To Account  : "); String to   = sc.nextLine().trim();
        System.out.print("Amount      : "); BigDecimal amt = readAmount();
        txnEngine.transfer(from, to, amt, "Fund Transfer");
        System.out.println("[Transfer] Completed successfully.");
    }

    private static void payBill() throws Exception {
        System.out.print("Account Number  : "); String acct = sc.nextLine().trim();
        System.out.print("Biller Name     : "); String biller = InputValidator.sanitise(sc.nextLine());
        System.out.print("Bill Reference  : "); String ref    = InputValidator.sanitise(sc.nextLine());
        System.out.print("Amount          : "); BigDecimal amt = readAmount();
        txnEngine.payBill(acct, amt, biller, ref);
        System.out.println("[Bill Payment] Paid successfully.");
    }

    private static void transactionHistory() throws AccountException {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        System.out.print("Search keyword (or Enter to skip): "); String kw = sc.nextLine().trim();
        var txns = txnEngine.getHistory(acct, kw);
        System.out.println("\n-- Transaction History --");
        if (txns.isEmpty()) System.out.println("  No transactions found.");
        else txns.forEach(System.out::println);
    }

    private static void miniStatement() throws Exception {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        reportService.printMiniStatement(acct);
    }

    private static void fullStatement() throws Exception {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        System.out.print("From (YYYY-MM-DD): "); LocalDate from = LocalDate.parse(sc.nextLine().trim());
        System.out.print("To   (YYYY-MM-DD): "); LocalDate to   = LocalDate.parse(sc.nextLine().trim());
        reportService.printFullStatement(acct, from, to);
    }

    private static void exportStatement() throws Exception {
        System.out.print("Account Number  : "); String acct = sc.nextLine().trim();
        System.out.print("From (YYYY-MM-DD): "); LocalDate from = LocalDate.parse(sc.nextLine().trim());
        System.out.print("To   (YYYY-MM-DD): "); LocalDate to   = LocalDate.parse(sc.nextLine().trim());
        String file = "statement_" + acct + "_" + from + "_" + to + ".txt";
        reportService.exportStatementToFile(acct, from, to, file);
    }

    // -- Loan operations -------------------------------------------------------

    private static void applyLoan(Customer cust) {
        System.out.print("Loan Amount    : "); BigDecimal principal = readAmount();
        System.out.print("Tenure (months): "); int tenure = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Monthly Income : "); BigDecimal income = readAmount();
        loanService.applyForLoan(cust, principal, 0.12, tenure, income);  // 12 % p.a.
    }

    private static void viewLoans(Customer cust) {
        var myLoans = loanService.getCustomerLoans(cust);
        System.out.println("\n-- My Loans --");
        if (myLoans.isEmpty()) { System.out.println("  No loans found."); return; }
        myLoans.forEach(l -> {
            System.out.println("  " + l);
            l.printSchedule();
        });
    }

    private static void makeLoanRepayment() throws AccountException {
        System.out.print("Loan ID       : "); String loanId = sc.nextLine().trim();
        System.out.print("Account Number: "); String acct   = sc.nextLine().trim();
        loanService.makeRepayment(loanId, acct);
    }

    private static void emiCalculator() {
        System.out.print("Principal      : "); BigDecimal p = readAmount();
        System.out.print("Annual Rate (%) : "); double rate = Double.parseDouble(sc.nextLine().trim()) / 100;
        System.out.print("Tenure (months) : "); int months = Integer.parseInt(sc.nextLine().trim());
        EMICalculator.printSummary(p, rate, months);
    }

    // -- Admin-only ------------------------------------------------------------

    private static void registerTeller() throws AuthException {
        System.out.print("Full Name  : "); String name   = sc.nextLine().trim();
        System.out.print("Email      : "); String email  = sc.nextLine().trim();
        System.out.print("Phone      : "); String phone  = sc.nextLine().trim();
        System.out.print("Password   : "); String pass   = sc.nextLine().trim();
        System.out.print("Branch Code: "); String branch = sc.nextLine().trim();
        authService.registerTeller(name, email, phone, pass, branch);
        System.out.println("[Admin] Teller registered.");
    }

    private static void suspendAccount() throws AccountException {
        System.out.print("Account Number: "); String acct   = sc.nextLine().trim();
        System.out.print("Reason        : "); String reason = sc.nextLine().trim();
        accountService.suspendAccount(acct, reason);
    }

    private static void reactivateAccount() throws AccountException {
        System.out.print("Account Number: "); String acct = sc.nextLine().trim();
        accountService.reactivateAccount(acct);
        System.out.println("[Admin] Account reactivated.");
    }

    private static void unlockUserAccount() throws AuthException {
        System.out.print("Target Email: "); String email = sc.nextLine().trim();
        authService.unlockAccount(sessionToken, email);
    }

    // ==========================================================================
    //  Utility helpers
    // ==========================================================================

    private static BigDecimal readAmount() {
        while (true) {
            String input = sc.nextLine().trim();
            if (InputValidator.isValidAmount(input))
                return new BigDecimal(input);
            System.out.print("Invalid amount. Try again: ");
        }
    }

    private static void printBanner() {
        System.out.println("+==================================================+");
        System.out.println("|   SECURE FINANCIAL SERVICES MANAGEMENT SYSTEM   |");
        System.out.println("|              SecureBank v1.0                    |");
        System.out.println("+==================================================+");
    }

    /**
     * Pre-seeds one admin account so testers can log in immediately.
     * Credentials: admin@securebank.com / Admin@1234
     */
    private static void seedDemoData() {
        try {
            authService.registerAdmin("Super Admin", "admin@securebank.com",
                    "+923001234567", "Admin@1234", 2);
            System.out.println("[Seed] Demo admin created: admin@securebank.com / Admin@1234");
        } catch (Exception e) {
            // Already seeded
        }
    }
}
