package com.tddai.broker.service;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.order.TimeInForce;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.portfolio.Portfolio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuyingPowerValidatorTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private final BuyingPowerValidator validator = new BuyingPowerValidator(Money.usd("1.00"));

    /**
     * 10 shares at a 235.00 limit: the order can cost at most 2350.00, plus the flat 1.00 fee.
     */
    private static OrderRequest buyTenAtLimit235() {
        return new OrderRequest(
                ACCOUNT_ID,
                Symbol.of("AAPL"),
                Side.BUY, 10,
                Money.usd("235.00"),
                TimeInForce.FOK
        );
    }

    private static Portfolio withBalances(CashBalance... balances) {
        return new Portfolio(ACCOUNT_ID, List.of(balances), List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5000", "2351.00"})
    void canAffordABuyWhenTheUsdBalanceCoversCostPlusFee(String usdBalance) {
        var order = buyTenAtLimit235();
        var portfolio = withBalances(CashBalance.usd(usdBalance));

        assertTrue(validator.canAfford(order, portfolio));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2350.99", "2350.00", "0"})
    void cannotAffordABuyWhenTheUsdBalanceIsShortOfCostPlusFee(String usdBalance) {
        var order = buyTenAtLimit235();
        var portfolio = withBalances(CashBalance.usd(usdBalance));

        assertFalse(validator.canAfford(order, portfolio));
    }

    @Test
    void anEurBalanceNeverHelpsPayForABuy() {
        // trading goes through USD only: plenty of EUR does not make the order affordable
        var order = buyTenAtLimit235();
        var portfolio = withBalances(CashBalance.usd("100"), CashBalance.eur("10000"));

        assertFalse(validator.canAfford(order, portfolio));
    }
}
