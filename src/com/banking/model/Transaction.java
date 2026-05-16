package com.banking.model;

import com.banking.security.SecurityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents a single financial transaction with receipt generation
 * and account-number masking capabilities.
 */
public class Transaction {

    private final String transactionId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private TransactionStatus status;
    private final String description;
    private final String accountNumber;   // stored in plain form; masked in output

    public Transaction(TransactionType type, BigDecimal amount,
                       String accountNumber, String description) {
        this.transactionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.type        = type;
        this.amount      = amount;
        this.accountNumber = accountNumber;
        this.description = description;
        this.timestamp   = LocalDateTime.now();
        this.status      = TransactionStatus.COMPLETED;
    }

    // -- Getters --------------------------------------------------------------

    public String          getTransactionId() { return transactionId; }
    public TransactionType getType()          { return type; }
    public BigDecimal      getAmount()        { return amount; }
    public LocalDateTime   getTimestamp()     { return timestamp; }
    public TransactionStatus getStatus()      { return status; }
    public String          getDescription()   { return description; }
    public String          getAccountNumber() { return accountNumber; }

    public void setStatus(TransactionStatus status) { this.status = status; }

    /** Returns the account number as ****XXXX (last 4 digits visible). */
    public String maskAccountNumber() {
        return SecurityService.maskAccountNumber(accountNumber);
    }

    /**
     * Prints a formatted receipt to stdout (console simulation of SMS/email).
     */
    public void generateReceipt() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
        System.out.println("\n+======================================+");
        System.out.println("|         TRANSACTION RECEIPT          |");
        System.out.println("+======================================+");
        System.out.printf( "| Txn ID   : %-26s|%n", transactionId);
        System.out.printf( "| Type     : %-26s|%n", type);
        System.out.printf( "| Amount   : PKR %-23.2f|%n", amount);
        System.out.printf( "| Account  : %-26s|%n", maskAccountNumber());
        System.out.printf( "| Date/Time: %-26s|%n", timestamp.format(fmt));
        System.out.printf( "| Status   : %-26s|%n", status);
        System.out.printf( "| Desc     : %-26s|%n",
                description.length() > 26 ? description.substring(0, 23) + "..." : description);
        System.out.println("+======================================+");
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        return String.format("[%s] %-16s PKR %,12.2f  %-10s %s",
                timestamp.format(fmt), type, amount, status, description);
    }
}
