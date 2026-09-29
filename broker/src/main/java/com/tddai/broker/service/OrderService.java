package com.tddai.broker.service;

import com.tddai.broker.domain.order.ExecutionReport;
import com.tddai.broker.domain.order.Order;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.order.RejectionReason;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.error.AccountNotFoundException;
import com.tddai.broker.infra.ExchangeClient;
import com.tddai.broker.infra.PortfolioStore;

import java.util.Optional;

/**
 * Orchestrates one order: validate, check a buy is affordable, send it to the exchange, and if it
 * was filled, update and save the portfolio.
 */
public class OrderService {

    private final PortfolioStore portfolioStore;
    private final OrderValidator orderValidator;
    private final BuyingPowerValidator buyingPowerValidator;
    private final ExchangeClient exchangeClient;
    private final PortfolioUpdater portfolioUpdater;

    public OrderService(PortfolioStore portfolioStore,
                        OrderValidator orderValidator,
                        BuyingPowerValidator buyingPowerValidator,
                        ExchangeClient exchangeClient,
                        PortfolioUpdater portfolioUpdater) {
        this.portfolioStore = portfolioStore;
        this.orderValidator = orderValidator;
        this.buyingPowerValidator = buyingPowerValidator;
        this.exchangeClient = exchangeClient;
        this.portfolioUpdater = portfolioUpdater;
    }

    /**
     * Returns the outcome: FILLED (the exchange executed it), REJECTED (the broker refused to send
     * it), or KILLED (the exchange received it but did not fill it).
     * The portfolio is saved only when filled.
     */
    public Order placeOrder(OrderRequest request) {
        Portfolio portfolio = portfolioStore.findPortfolio(request.accountId())
                .orElseThrow(() -> new AccountNotFoundException(request.accountId().toString()));

        Optional<RejectionReason> invalid = orderValidator.validate(request, portfolio);
        if (invalid.isPresent()) {
            return Order.rejected(request, invalid.get());
        }
        if (request.side() == Side.BUY && !buyingPowerValidator.canAfford(request, portfolio)) {
            return Order.rejected(request, RejectionReason.INSUFFICIENT_FUNDS);
        }

        ExecutionReport report = exchangeClient.submitOrder(request);

        return switch (report) {
            case ExecutionReport.Killed killed -> Order.killed(request, killed.reason());
            case ExecutionReport.Filled filled -> {
                Order order = Order.filled(request, filled.price(), filled.timestamp());
                portfolioStore.save(portfolioUpdater.applyFill(portfolio, order));
                yield order;
            }
        };
    }
}
