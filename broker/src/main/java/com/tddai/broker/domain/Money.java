package com.tddai.broker.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An amount in a currency: everywhere a monetary amount appears that is not an account's held
 * balance (quote price, limit price, fill price, cost basis).
 * Equality ignores scale: 100 equals 100.00.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }

    public static Money usd(String amount) {
        return new Money(new BigDecimal(amount), Currency.USD);
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, Currency.USD);
    }

    public Money times(long quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money(
                BigDecimal amount1, Currency currency1
        ) && currency == currency1 && amount.compareTo(amount1) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }
}
