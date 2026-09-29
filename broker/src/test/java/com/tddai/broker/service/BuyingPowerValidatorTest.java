package com.tddai.broker.service;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.order.TimeInForce;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.portfolio.Portfolio;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuyingPowerValidatorTest {

    private static final UUID ACCOUNT = UUID.randomUUID();

    private final BuyingPowerValidator validator = new BuyingPowerValidator();

    /**
     * 10 shares at a 235.00 limit: the order can cost at most 2350.00.
     */
    private static OrderRequest buyTenAtLimit235() {
        return new OrderRequest(ACCOUNT, Symbol.of("AAPL"), Side.BUY, 10, Money.usd("235.00"), TimeInForce.FOK);
    }

    private static Portfolio withCash(CashBalance... balances) {
        return new Portfolio(ACCOUNT, List.of(balances), List.of());
    }

    @Test
    void canAffordABuyWhenTheUsdBalanceIsLargeEnough() {
        assertTrue(validator.canAfford(buyTenAtLimit235(), withCash(CashBalance.usd("5000"))));
    }

    @Test
    void canAffordABuyCostingExactlyTheUsdBalance() {
        assertTrue(validator.canAfford(buyTenAtLimit235(), withCash(CashBalance.usd("2350.00"))));
    }

    @Test
    void cannotAffordABuyCostingEvenACentMoreThanTheUsdBalance() {
        assertFalse(validator.canAfford(buyTenAtLimit235(), withCash(CashBalance.usd("2349.99"))));
    }

    @Test
    void cannotAffordAnythingWithNoUsdBalanceAtAll() {
        assertFalse(validator.canAfford(buyTenAtLimit235(), withCash()));
    }

    @Test
    void anEurBalanceNeverHelpsPayForABuy() {
        // trading goes through USD only: plenty of EUR does not make the order affordable
        assertFalse(validator.canAfford(buyTenAtLimit235(),
                withCash(CashBalance.usd("100"), CashBalance.eur("10000"))));
    }
}
