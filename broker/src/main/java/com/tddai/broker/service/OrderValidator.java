package com.tddai.broker.service;

import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.order.RejectionReason;
import com.tddai.broker.domain.order.Side;

import java.util.Optional;

/**
 * Refuses a sell of more shares than the portfolio holds.
 * The request's own fields are already valid: {@link OrderRequest} checks them when it is created.
 */
public class OrderValidator {

    public Optional<RejectionReason> validate(OrderRequest request, Portfolio portfolio) {
        if (request.side() == Side.SELL) {
            long held = portfolio.position(request.symbol()).map(Position::quantity).orElse(0L);
            if (held < request.quantity()) {
                return Optional.of(RejectionReason.INSUFFICIENT_POSITION);
            }
        }
        return Optional.empty();
    }
}
