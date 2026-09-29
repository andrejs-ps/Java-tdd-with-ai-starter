package com.tddai.broker.domain;

import com.tddai.broker.error.InvalidRequestException;

import java.util.regex.Pattern;

/**
 * A validated ticker: 1 to 5 upper-case letters.
 * No RIC / ISIN / CUSIP.
 */
public record Symbol(String value) {

    private static final Pattern TICKER = Pattern.compile("[A-Z]{1,5}");

    public Symbol {
        if (value == null || !TICKER.matcher(value).matches()) {
            throw new InvalidRequestException("Invalid symbol: " + value);
        }
    }

    public static Symbol of(String value) {
        return new Symbol(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
