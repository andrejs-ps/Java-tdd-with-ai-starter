package com.tddai.exchange.order;

import com.tddai.exchange.quote.QuoteDto;
import com.tddai.exchange.quote.QuoteGenerator;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Executes an order against the hard-coded prices (the same ones served as quotes).
 * Fill or kill: a BUY executes if the price is at or below the limit, a SELL if it is at or above;
 * otherwise the order is killed.
 * An order for a symbol the exchange does not list is killed too: there is nothing to fill it with.
 */
public class OrderExecutor {

    private final QuoteGenerator quotes;
    private final Clock clock;

    public OrderExecutor(QuoteGenerator quotes, Clock clock) {
        this.quotes = quotes;
        this.clock = clock;
    }

    public ExecutionDto execute(OrderRequestDto order) {
        String orderId = UUID.randomUUID().toString();
        Optional<QuoteDto> quote = quotes.quoteFor(order.symbol());
        if (quote.isEmpty()) {
            return killed(orderId, order, KillReason.UNKNOWN_SYMBOL);
        }

        BigDecimal price = quote.get().price();
        boolean withinLimit = order.side() == Side.BUY
                ? price.compareTo(order.limitPrice()) <= 0
                : price.compareTo(order.limitPrice()) >= 0;
        if (!withinLimit) {
            return killed(orderId, order, KillReason.LIMIT_NOT_MET);
        }
        return new ExecutionDto(orderId, order.clientOrderId(), ExecutionStatus.FILLED, order.symbol(), order.side(), order.quantity(),
                price, null, clock.instant());
    }

    private ExecutionDto killed(String orderId, OrderRequestDto order, KillReason reason) {
        return new ExecutionDto(orderId, order.clientOrderId(), ExecutionStatus.KILLED, order.symbol(), order.side(), order.quantity(),
                null, reason, clock.instant());
    }
}
