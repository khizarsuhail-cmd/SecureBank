package com.banking.exception;

/**
 * Thrown for authentication and authorisation failures.
 */
public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }
}
