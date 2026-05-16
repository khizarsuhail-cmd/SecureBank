package com.banking.exception;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal or transfer has insufficient funds.
 */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super(String.format("Insufficient funds. Available: %.2f, Requested: %.2f",
                available, requested));
    }
}
