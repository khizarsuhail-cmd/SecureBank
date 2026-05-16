package com.banking.model;

import com.banking.loan.EMICalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a loan product with EMI schedule and status tracking.
 */
public class Loan {

    private final String loanId;
    private final Customer borrower;
    private final BigDecimal principal;
    private final double annualRate;
    private final int tenureMonths;
    private LoanStatus status;
    private final int creditScore;
    private final List<Installment> repaymentSchedule;
    private final LocalDate applicationDate;

    public Loan(Customer borrower, BigDecimal principal,
                double annualRate, int tenureMonths, int creditScore) {
        this.loanId           = "LN" + UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase();
        this.borrower         = borrower;
        this.principal        = principal;
        this.annualRate       = annualRate;
        this.tenureMonths     = tenureMonths;
        this.creditScore      = creditScore;
        this.status           = LoanStatus.PENDING;
        this.repaymentSchedule = new ArrayList<>();
        this.applicationDate  = LocalDate.now();
    }

    /** Computes the monthly EMI using the standard formula. */
    public BigDecimal calculateEMI() {
        return EMICalculator.computeEMI(principal, annualRate, tenureMonths);
    }

    /**
     * Builds the full repayment schedule once the loan is approved.
     * Each instalment is due on the same day of every subsequent month.
     */
    public void generateSchedule() {
        repaymentSchedule.clear();
        BigDecimal emi = calculateEMI();
        LocalDate  due = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= tenureMonths; i++) {
            repaymentSchedule.add(new Installment(i, due, emi));
            due = due.plusMonths(1);
        }
    }

    /** Prints the full repayment schedule to the console. */
    public void printSchedule() {
        System.out.println("\n-- Repayment Schedule for Loan " + loanId + " --");
        System.out.printf("  Principal: PKR %,.2f | Rate: %.2f%% | Tenure: %d months | EMI: PKR %,.2f%n",
                principal, annualRate * 100, tenureMonths, calculateEMI());
        repaymentSchedule.forEach(System.out::println);
    }

    /** Returns the next unpaid instalment, or null if all paid. */
    public Installment getNextDueInstallment() {
        return repaymentSchedule.stream()
                .filter(i -> !i.isPaid())
                .findFirst()
                .orElse(null);
    }

    /** Returns all overdue, unpaid instalments. */
    public List<Installment> getOverdueInstallments() {
        return repaymentSchedule.stream()
                .filter(Installment::isPastDue)
                .collect(java.util.stream.Collectors.toList());
    }

    // -- Getters / Setters -----------------------------------------------------

    public String           getLoanId()            { return loanId; }
    public Customer         getBorrower()           { return borrower; }
    public BigDecimal       getPrincipal()          { return principal; }
    public double           getAnnualRate()         { return annualRate; }
    public int              getTenureMonths()       { return tenureMonths; }
    public LoanStatus       getStatus()             { return status; }
    public int              getCreditScore()        { return creditScore; }
    public List<Installment>getRepaymentSchedule()  { return new ArrayList<>(repaymentSchedule); }
    public LocalDate        getApplicationDate()    { return applicationDate; }

    public void setStatus(LoanStatus s) { this.status = s; }

    @Override
    public String toString() {
        return String.format("Loan[%s | PKR %,.2f | %.2f%% | %d mo | %s | Score:%d]",
                loanId, principal, annualRate * 100, tenureMonths, status, creditScore);
    }
}
