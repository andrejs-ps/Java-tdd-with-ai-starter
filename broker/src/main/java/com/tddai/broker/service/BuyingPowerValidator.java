package com.tddai.broker.service;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.portfolio.Portfolio;

/**
 * Can the account pay for a buy?
 * Trading goes through USD only: the order must be affordable from the USD balance, and any EUR
 * balance is ignored.
 */
public class BuyingPowerValidator {

    /**
     * True when the USD balance covers the order's worst-case cost, which is the limit price times
     * the quantity (an order never fills at a worse price than its limit).
     * Exactly equal is enough.
     */
    public boolean canAfford(OrderRequest order, Portfolio portfolio) {
        Money worstCaseCost = order.limitPrice().times(order.quantity());
        return worstCaseCost.amount().compareTo(portfolio.cash(Currency.USD)) <= 0;
    }
}
