package com.tddai.broker.domain.order;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.error.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderRequestTest {

    private static final UUID ACCOUNT = UUID.randomUUID();
    private static final Symbol AAPL = Symbol.of("AAPL");

    private static OrderRequest order(long quantity, Money limitPrice) {
        return new OrderRequest(ACCOUNT, AAPL, Side.BUY, quantity, limitPrice, TimeInForce.FOK);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -5})
    void quantityMustBeAboveZero(long quantity) {
        assertThrows(InvalidRequestException.class, () -> order(quantity, Money.usd("235.00")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00"})
    void limitPriceMustBeAboveZero(String limitPrice) {
        assertThrows(InvalidRequestException.class, () -> order(1, Money.usd(limitPrice)));
    }

    @Test
    void limitPriceMustBeInUsd() {
        var eur = new Money(new BigDecimal("235.00"), Currency.EUR);

        assertThrows(InvalidRequestException.class, () -> order(1, eur));
    }
}
