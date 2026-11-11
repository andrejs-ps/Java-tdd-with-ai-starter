package com.tddai.broker.http;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.error.AccountNotFoundException;
import com.tddai.broker.error.InvalidRequestException;

import java.util.UUID;

/**
 * Small parsing helpers shared by the controllers; every failure maps to a domain exception.
 */
final class Requests {

    private Requests() {
    }

    /**
     * A malformed id cannot belong to any account, so it is a 404 rather than a 400.
     */
    static UUID accountId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException(raw);
        }
    }

    static Currency currency(String raw) {
        if (raw == null) {
            throw new InvalidRequestException("currency is required");
        }
        try {
            return Currency.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unsupported currency: " + raw);
        }
    }

    static <T extends Enum<T>> T enumValue(Class<T> type, String raw, String field) {
        if (raw == null) {
            throw new InvalidRequestException(field + " is required");
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid " + field + ": " + raw);
        }
    }

    static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidRequestException(field + " is required");
        }
        return value;
    }
}
