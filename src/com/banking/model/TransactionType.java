package com.banking.model;

/**
 * Types of financial transactions supported by the system.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    BILL_PAYMENT,
    INTEREST_CREDIT,
    LOAN_DISBURSEMENT,
    LOAN_REPAYMENT
}
