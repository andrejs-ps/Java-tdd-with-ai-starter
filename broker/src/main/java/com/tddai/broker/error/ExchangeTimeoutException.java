package com.tddai.broker.error;

/**
 * The exchange did not answer in time.
 * HTTP 504.
 */
public class ExchangeTimeoutException extends RuntimeException {
    public ExchangeTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
