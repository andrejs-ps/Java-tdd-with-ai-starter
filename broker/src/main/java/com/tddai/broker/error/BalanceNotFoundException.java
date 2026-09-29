package com.tddai.broker.error;

/**
 * The account exists but has no balance in that currency.
 * HTTP 404.
 */
public class BalanceNotFoundException extends RuntimeException {
    public BalanceNotFoundException(String currency) {
        super("No open " + currency + " balance on this account");
    }
}
