package com.tddai.broker.domain.order;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;

import java.util.UUID;

/**
 * An order as submitted, before any decision has been made about it.
 * The id is the client order id: the broker creates it, sends it to the exchange, and it stays the
 * id of the resulting Order.
 */
public record OrderRequest(
        UUID id,
        UUID accountId,
        Symbol symbol,
        Side side,
        long quantity,
        Money limitPrice,
        TimeInForce timeInForce) {

    /**
     * A new order request with a freshly created client order id.
     */
    public OrderRequest(UUID accountId, Symbol symbol, Side side, long quantity, Money limitPrice,
                        TimeInForce timeInForce) {
        this(UUID.randomUUID(), accountId, symbol, side, quantity, limitPrice, timeInForce);
    }
}
