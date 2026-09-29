package com.tddai.broker.infra;

import com.tddai.broker.domain.Currency;

import java.math.BigDecimal;

/**
 * Static reference data, loaded once at startup.
 * Typed methods on purpose, not a key-value lookup.
 */
public interface StaticDataProvider {

    /**
     * The multiplier that converts an amount in {@code from} into {@code to}.
     */
    BigDecimal getFxRate(Currency from, Currency to);
}
