package com.tddai.broker.domain.portfolio;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    private static final Symbol AAPL = Symbol.of("AAPL");

    @Test
    void aPositionNeedsAtLeastOneShare() {
        assertDoesNotThrow(() -> new Position(AAPL, 1, Money.usd("100")));
        assertThrows(IllegalArgumentException.class, () -> new Position(AAPL, 0, Money.usd("100")));
        assertThrows(IllegalArgumentException.class, () -> new Position(AAPL, -3, Money.usd("100")));
    }

    @Test
    void theCostBasisCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Position(AAPL, 1, Money.usd("-0.01")));
    }

    @Test
    void aZeroCostBasisIsAllowed() {
        assertDoesNotThrow(() -> new Position(AAPL, 1, Money.usd("0")));
    }
}
