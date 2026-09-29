package com.tddai.broker.service;

import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.order.Order;
import com.tddai.broker.domain.order.OrderStatus;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure: applies an already FILLED order to a portfolio and returns the new portfolio. No I/O.
 * <p>
 * Trading goes through USD only; any EUR balance is left untouched.
 * BUY: debit fillPrice x quantity from USD; add to the position and to its total cost basis.
 * SELL: credit USD; reduce quantity; reduce cost basis at average cost, HALF_EVEN, 2 decimals.
 * <p>
 * No affordability or stock checks live here: the caller has already made sure the order is covered. The portfolio
 * types guarantee the rest, because a negative cash balance or a position without shares cannot be constructed.
 */
public class PortfolioUpdater {

    /**
     * Updates the investor's portfolio after an order has been filled, and returns the updated portfolio.
     * <p>
     * A buy takes the money out of the USD balance and adds the shares to the position. A sell puts the money
     * into the USD balance and takes the shares out of the position. EUR balances are never touched.
     * <p>
     * The portfolio you pass in is left unchanged; a new one is returned. Nothing is checked here: the caller must
     * already have made sure there is enough cash for a buy and enough shares for a sell. If not, the result would
     * be a negative balance or position, which the portfolio types refuse to build (IllegalArgumentException).
     *
     * @param portfolio the portfolio before the order
     * @param order     an order that was filled (it has a fill price)
     * @return the portfolio after the order
     */
    public Portfolio applyFill(Portfolio portfolio, Order order) {
        if (order.status() != OrderStatus.FILLED) {
            throw new IllegalArgumentException("Only FILLED orders can be applied");
        }
        Money value = order.fillPrice().times(order.quantity());
        return switch (order.side()) {
            case BUY -> applyBuy(portfolio, order, value);
            case SELL -> applySell(portfolio, order, value);
        };
    }

    private Portfolio applyBuy(Portfolio portfolio, Order order, Money value) {
        Portfolio updated = portfolio.withCashBalance(
                new CashBalance(portfolio.cash(Currency.USD).subtract(value.amount()), Currency.USD));

        Money newCostBasis = portfolio.position(order.symbol())
                .map(p -> p.costBasis().plus(value))
                .orElse(value);
        long newQuantity = portfolio.position(order.symbol()).map(Position::quantity).orElse(0L) + order.quantity();
        return updated.withPosition(new Position(order.symbol(), newQuantity, newCostBasis));
    }

    private Portfolio applySell(Portfolio portfolio, Order order, Money value) {
        Position held = portfolio.position(order.symbol())
                .orElseThrow(() -> new IllegalStateException("No position in " + order.symbol() + " to sell from"));

        Portfolio updated = portfolio.withCashBalance(
                new CashBalance(portfolio.cash(Currency.USD).add(value.amount()), Currency.USD));

        long remaining = held.quantity() - order.quantity();
        if (remaining == 0) {
            return updated.withoutPosition(order.symbol());
        }
        BigDecimal remainingCost = held.costBasis().amount()
                .multiply(BigDecimal.valueOf(remaining))
                .divide(BigDecimal.valueOf(held.quantity()), 2, RoundingMode.HALF_EVEN);
        return updated.withPosition(new Position(order.symbol(), remaining, new Money(remainingCost, held.costBasis().currency())));
    }
}
