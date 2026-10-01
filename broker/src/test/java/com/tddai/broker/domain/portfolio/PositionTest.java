package com.tddai.broker.domain.portfolio;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    private static final Symbol AAPL = Symbol.of("AAPL");

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shareCountMustBeAboveZero(long quantity) {
        assertThrows(IllegalArgumentException.class, () -> new Position(AAPL, quantity, Money.usd("100")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01"})
    void theCostBasisMustBeAboveZero(String costBasis) {
        assertThrows(IllegalArgumentException.class, () -> new Position(AAPL, 1, Money.usd(costBasis)));
    }
}
