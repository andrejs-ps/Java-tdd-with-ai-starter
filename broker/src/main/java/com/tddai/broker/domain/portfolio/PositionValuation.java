package com.tddai.broker.domain.portfolio;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;

/**
 * A Position valued at a live quote.
 * Never stored.
 */
public record PositionValuation(
        Symbol symbol,
        long quantity,
        Money costBasis,
        Money marketValue,
        Money unrealizedGainLoss) {
}
