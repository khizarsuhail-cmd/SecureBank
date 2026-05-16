package com.banking.exception;

/**
 * General-purpose exception for account-related errors.
 */
public class AccountException extends Exception {
    public AccountException(String message) {
        super(message);
    }
}
