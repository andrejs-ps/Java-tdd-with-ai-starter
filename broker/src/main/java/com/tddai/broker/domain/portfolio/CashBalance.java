package com.tddai.broker.domain.portfolio;

import com.tddai.broker.domain.Currency;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * What a Portfolio holds in one currency.
 * Same shape as Money, kept distinct for domain clarity.
 * A balance can be zero but never negative: an invalid balance cannot be built.
 */
public record CashBalance(BigDecimal amount, Currency currency) {

    public CashBalance {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("A cash balance cannot be negative: " + amount + " " + currency);
        }
    }

    public static CashBalance usd(String amount) {
        return new CashBalance(new BigDecimal(amount), Currency.USD);
    }

    public static CashBalance eur(String amount) {
        return new CashBalance(new BigDecimal(amount), Currency.EUR);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CashBalance(
                BigDecimal amount1, Currency currency1
        ) && currency == currency1 && amount.compareTo(amount1) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }
}
