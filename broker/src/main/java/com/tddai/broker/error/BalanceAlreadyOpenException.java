package com.tddai.broker.error;

/**
 * HTTP 409.
 */
public class BalanceAlreadyOpenException extends RuntimeException {
    public BalanceAlreadyOpenException(String currency) {
        super(currency + " balance is already open on this account");
    }
}
