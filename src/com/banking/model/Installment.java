package com.banking.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single EMI instalment in a loan repayment schedule.
 */
public class Installment {

    private final int installmentNo;
    private final LocalDate dueDate;
    private final BigDecimal emiAmount;
    private boolean paid;

    public Installment(int installmentNo, LocalDate dueDate, BigDecimal emiAmount) {
        this.installmentNo = installmentNo;
        this.dueDate       = dueDate;
        this.emiAmount     = emiAmount;
        this.paid          = false;
    }

    /** Returns true if this instalment is overdue and unpaid. */
    public boolean isPastDue() {
        return !paid && LocalDate.now().isAfter(dueDate);
    }

    public void markPaid() { this.paid = true; }

    public int         getInstallmentNo() { return installmentNo; }
    public LocalDate   getDueDate()       { return dueDate; }
    public BigDecimal  getEmiAmount()     { return emiAmount; }
    public boolean     isPaid()           { return paid; }

    @Override
    public String toString() {
        return String.format("  EMI #%-3d | Due: %s | PKR %,.2f | %s",
                installmentNo, dueDate, emiAmount, paid ? "PAID" : (isPastDue() ? "OVERDUE" : "PENDING"));
    }
}
