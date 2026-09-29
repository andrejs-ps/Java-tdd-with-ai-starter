package com.tddai.broker.domain.order;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Symbol;

import java.time.Instant;
import java.util.UUID;

/**
 * The outcome of an order, one of three: FILLED (fillPrice and filledAt set), REJECTED by the
 * broker before anything was sent (rejectionReason set), or KILLED by the exchange, which did not
 * fill it (killReason set).
 * There is no pending state; an Order is never persisted.
 */
public record Order(
        UUID id,
        UUID accountId,
        Symbol symbol,
        Side side,
        long quantity,
        Money limitPrice,
        TimeInForce timeInForce,
        Money fillPrice,
        Instant filledAt,
        OrderStatus status,
        RejectionReason rejectionReason,
        KillReason killReason) {

    public static Order filled(OrderRequest request, Money fillPrice, Instant filledAt) {
        return new Order(request.id(), request.accountId(), request.symbol(), request.side(),
                request.quantity(), request.limitPrice(), request.timeInForce(),
                fillPrice, filledAt, OrderStatus.FILLED, null, null);
    }

    public static Order rejected(OrderRequest request, RejectionReason reason) {
        return new Order(request.id(), request.accountId(), request.symbol(), request.side(),
                request.quantity(), request.limitPrice(), request.timeInForce(),
                null, null, OrderStatus.REJECTED, reason, null);
    }

    public static Order killed(OrderRequest request, KillReason reason) {
        return new Order(request.id(), request.accountId(), request.symbol(), request.side(),
                request.quantity(), request.limitPrice(), request.timeInForce(),
                null, null, OrderStatus.KILLED, null, reason);
    }
}
