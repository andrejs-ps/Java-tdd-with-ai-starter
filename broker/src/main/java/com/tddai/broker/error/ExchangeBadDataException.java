package com.tddai.broker.error;

/**
 * The exchange answered with an error status, or could not be reached.
 * HTTP 502.
 */
public class ExchangeBadDataException extends RuntimeException {
    public ExchangeBadDataException(String message) {
        super(message);
    }

    public ExchangeBadDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
