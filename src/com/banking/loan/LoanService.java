package com.banking.loan;

import com.banking.account.AccountService;
import com.banking.exception.AccountException;
import com.banking.model.*;
import com.banking.security.AuditLog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Module 4 - Loan & Credit Services.
 *
 * Handles loan applications with credit scoring, approval/rejection,
 * EMI scheduling, repayment processing, and overdue detection.
 */
public class LoanService {

    private final AccountService accountService;
    private final Map<String, Loan> loans = new HashMap<>();
    private final AuditLog audit = AuditLog.getInstance();

    // Credit score thresholds
    private static final int MIN_CREDIT_SCORE_APPROVE = 600;
    private static final int MAX_LOAN_TO_INCOME_RATIO  = 10;   // loan <= 10x monthly income

    public LoanService(AccountService accountService) {
        this.accountService = accountService;
    }

    // -- Application -----------------------------------------------------------

    /**
     * Submits a loan application and computes a credit score.
     * Automatically approves or rejects based on the score.
     *
     * @param customer       applicant
     * @param principal      requested loan amount
     * @param annualRate     offered annual rate (set by bank policy)
     * @param tenureMonths   repayment tenure
     * @param monthlyIncome  declared monthly income (used for scoring)
     * @return the created Loan object
     */
    public Loan applyForLoan(Customer customer, BigDecimal principal,
                              double annualRate, int tenureMonths,
                              BigDecimal monthlyIncome) {
        int creditScore = computeCreditScore(customer, principal, monthlyIncome);
        Loan loan = new Loan(customer, principal, annualRate, tenureMonths, creditScore);
        loans.put(loan.getLoanId(), loan);
        customer.addLoan(loan);

        audit.log(customer.getUserId(), "LOAN_APPLICATION",
                "loanId=" + loan.getLoanId() + " amount=" + principal + " score=" + creditScore);

        System.out.printf("[Loan] Application %s submitted. Credit Score: %d%n",
                loan.getLoanId(), creditScore);

        // Auto-approve / reject
        if (creditScore >= MIN_CREDIT_SCORE_APPROVE) {
            approveLoan(loan.getLoanId());
        } else {
            rejectLoan(loan.getLoanId(), "Credit score " + creditScore + " below minimum " + MIN_CREDIT_SCORE_APPROVE);
        }
        return loan;
    }

    // -- Approval / Rejection --------------------------------------------------

    public void approveLoan(String loanId) {
        Loan loan = loans.get(loanId);
        if (loan == null) { System.out.println("[Loan] Not found: " + loanId); return; }
        loan.setStatus(LoanStatus.APPROVED);
        loan.generateSchedule();
        audit.log(loan.getBorrower().getUserId(), "LOAN_APPROVED", loanId);
        System.out.println("[Loan] Approved: " + loanId);
        EMICalculator.printSummary(loan.getPrincipal(), loan.getAnnualRate(), loan.getTenureMonths());
    }

    public void rejectLoan(String loanId, String reason) {
        Loan loan = loans.get(loanId);
        if (loan == null) return;
        loan.setStatus(LoanStatus.REJECTED);
        audit.log(loan.getBorrower().getUserId(), "LOAN_REJECTED", loanId + " reason=" + reason);
        System.out.println("[Loan] Rejected: " + loanId + " - " + reason);
    }

    /**
     * Disburses the approved loan amount into the customer's account.
     */
    public void disburseLoan(String loanId, String targetAccountNumber)
            throws AccountException {
        Loan loan = loans.get(loanId);
        if (loan == null) throw new AccountException("Loan not found.");
        if (loan.getStatus() != LoanStatus.APPROVED)
            throw new AccountException("Loan must be APPROVED before disbursement.");

        accountService.getAccount(targetAccountNumber)
                .deposit(loan.getPrincipal(), "Loan disbursement: " + loanId);

        loan.setStatus(LoanStatus.ACTIVE);
        audit.log(loan.getBorrower().getUserId(), "LOAN_DISBURSED",
                loanId + " -> " + targetAccountNumber);
        System.out.printf("[Loan] PKR %,.2f disbursed for loan %s%n",
                loan.getPrincipal(), loanId);
    }

    // -- Repayment -------------------------------------------------------------

    /**
     * Processes the next EMI repayment from the customer's account.
     */
    public void makeRepayment(String loanId, String fromAccountNumber)
            throws AccountException {
        Loan loan = loans.get(loanId);
        if (loan == null) throw new AccountException("Loan not found.");
        if (loan.getStatus() != LoanStatus.ACTIVE)
            throw new AccountException("Loan is not ACTIVE.");

        Installment next = loan.getNextDueInstallment();
        if (next == null) {
            loan.setStatus(LoanStatus.CLOSED);
            System.out.println("[Loan] All instalments paid. Loan " + loanId + " CLOSED.");
            return;
        }

        try {
            accountService.getAccount(fromAccountNumber)
                    .withdraw(next.getEmiAmount(), "EMI repayment: " + loanId);
        } catch (Exception e) {
            throw new AccountException("Repayment failed: " + e.getMessage());
        }

        next.markPaid();
        audit.log(loan.getBorrower().getUserId(), "LOAN_REPAYMENT",
                loanId + " EMI#" + next.getInstallmentNo() + " PKR " + next.getEmiAmount());
        System.out.printf("[Loan] EMI #%d paid: PKR %,.2f for loan %s%n",
                next.getInstallmentNo(), next.getEmiAmount(), loanId);

        // Auto-close if all paid
        if (loan.getNextDueInstallment() == null) {
            loan.setStatus(LoanStatus.CLOSED);
            System.out.println("[Loan] Loan " + loanId + " fully repaid and CLOSED.");
        }
    }

    // -- Overdue Detection -----------------------------------------------------

    /**
     * Scans all active loans and flags/defaults overdue ones.
     */
    public void detectOverdue() {
        System.out.println("\n-- Overdue Scan --");
        for (Loan loan : loans.values()) {
            if (loan.getStatus() != LoanStatus.ACTIVE) continue;
            List<Installment> overdue = loan.getOverdueInstallments();
            if (!overdue.isEmpty()) {
                System.out.printf("  Loan %s - %d overdue instalment(s):%n",
                        loan.getLoanId(), overdue.size());
                overdue.forEach(System.out::println);
                audit.flagTransaction(loan.getBorrower().getUserId(),
                        "OVERDUE_LOAN " + overdue.size() + " instalments",
                        loan.getLoanId());
                // Default if severely overdue (>3 unpaid)
                if (overdue.size() > 3) {
                    loan.setStatus(LoanStatus.DEFAULTED);
                    System.out.println("  [!] Loan " + loan.getLoanId() + " marked DEFAULTED.");
                }
            }
        }
    }

    // -- Credit Scoring --------------------------------------------------------

    /**
     * Simple rule-based credit score (0-850).
     * In production this would call a bureau API.
     */
    private int computeCreditScore(Customer customer, BigDecimal principal,
                                   BigDecimal monthlyIncome) {
        int score = 700;   // base score

        // Penalise if no existing accounts
        if (customer.getAccounts().isEmpty()) score -= 50;

        // Penalise existing active loans
        long activeLoans = customer.getLoans().stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE).count();
        score -= (int)(activeLoans * 30);

        // Penalise if loan-to-income ratio is high
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            double lti = principal.doubleValue() / monthlyIncome.doubleValue();
            if (lti > MAX_LOAN_TO_INCOME_RATIO) score -= 100;
        }

        // Penalise any defaulted loans
        long defaults = customer.getLoans().stream()
                .filter(l -> l.getStatus() == LoanStatus.DEFAULTED).count();
        score -= (int)(defaults * 150);

        return Math.max(0, Math.min(850, score));
    }

    // -- Lookups ---------------------------------------------------------------

    public Loan getLoan(String loanId) { return loans.get(loanId); }

    public List<Loan> getCustomerLoans(Customer customer) {
        List<Loan> result = new ArrayList<>();
        for (Loan l : loans.values())
            if (l.getBorrower().getUserId().equals(customer.getUserId()))
                result.add(l);
        return result;
    }

    public Map<String, Loan> getAllLoans() { return new HashMap<>(loans); }
}
