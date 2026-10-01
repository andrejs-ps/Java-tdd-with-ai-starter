package com.tddai.broker.domain.portfolio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashBalanceTest {

    @Test
    void aBalanceCanBeZero() {
        assertDoesNotThrow(() -> CashBalance.usd("0"));
    }

    @Test
    void aBalanceCanBePositive() {
        assertEquals(new BigDecimal("12.50"), CashBalance.eur("12.50").amount());
    }

    @Test
    void aNegativeBalanceCannotBeBuilt() {
        assertThrows(IllegalArgumentException.class, () -> CashBalance.usd("-0.01"));
        assertThrows(IllegalArgumentException.class, () -> CashBalance.eur("-0.01"));
    }

    @Test
    void equalityIgnoresScale() {
        assertEquals(CashBalance.usd("100"), CashBalance.usd("100.00"));
    }
}
