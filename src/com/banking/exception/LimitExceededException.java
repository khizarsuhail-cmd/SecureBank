package com.banking.exception;

import java.math.BigDecimal;

/**
 * Thrown when a transaction would exceed the configured daily limit.
 */
public class LimitExceededException extends Exception {
    private final String limitType;
    private final BigDecimal attempted;
    private final BigDecimal limit;

    public LimitExceededException(String limitType, BigDecimal attempted, BigDecimal limit) {
        super(String.format("Daily %s limit exceeded. Attempted: %.2f, Limit: %.2f",
                limitType, attempted, limit));
        this.limitType = limitType;
        this.attempted = attempted;
        this.limit = limit;
    }

    public String getLimitType()    { return limitType; }
    public BigDecimal getAttempted(){ return attempted; }
    public BigDecimal getLimit()    { return limit; }
}
