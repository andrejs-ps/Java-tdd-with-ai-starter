package com.tddai.broker.error;

/**
 * The request itself is malformed or violates a basic rule.
 * HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
