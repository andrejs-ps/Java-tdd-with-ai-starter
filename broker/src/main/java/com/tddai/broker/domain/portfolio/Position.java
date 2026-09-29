package com.tddai.broker.domain.portfolio;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;

import java.util.Objects;

/**
 * A holding in one symbol. For example: AAPL, 10 shares, cost basis (avg purchase price: 230.00).
 * <p>
 * Market value and gain/loss are computed at read time from a live quote.
 * A position that is sold down to zero shares is removed from the portfolio.
 */
public record Position(Symbol symbol, long quantity, Money costBasis) {

    public Position {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(costBasis, "costBasis");
        if (quantity <= 0) {
            throw new IllegalArgumentException("A position needs at least one share: " + quantity);
        }
        if (costBasis.amount().signum() < 0) {
            throw new IllegalArgumentException("A position's cost basis cannot be negative: " + costBasis);
        }
    }
}
