package com.tddai.broker.error;

/**
 * HTTP 404.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}
