package com.tddai.broker.service;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.portfolio.Portfolio;

/**
 * Can the account pay for a buy?
 * Trading goes through USD only: the order and the broker's flat fee must be affordable from the USD
 * balance, and any EUR balance is ignored.
 */
public class BuyingPowerValidator {

    private final Money flatFee;

    public BuyingPowerValidator(Money flatFee) {
        this.flatFee = flatFee;
    }

    /**
     * True when the USD balance covers the order's max cost plus the flat fee.
     * The max cost is the limit price x quantity (an order never fills at a worse price than its limit).
     * Exactly equal is enough.
     */
    public boolean canAfford(OrderRequest order, Portfolio portfolio) {
        Money totalCost = order.limitPrice().times(order.quantity()).plus(flatFee);
        return totalCost.amount().compareTo(portfolio.cash(Currency.USD)) <= 0;
    }
}
