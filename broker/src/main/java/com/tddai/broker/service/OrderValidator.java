package com.tddai.broker.service;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.order.RejectionReason;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.error.InvalidRequestException;

import java.util.Optional;

/**
 * Order validation rules that need no exchange call and no cash check (affordability is
 * BuyingPowerValidator's job).
 * Malformed orders throw InvalidRequestException (HTTP 400); a SELL of more than the position holds
 * is returned as a reason (HTTP 422).
 */
public class OrderValidator {

    public Optional<RejectionReason> validate(OrderRequest request, Portfolio portfolio) {
        if (request.quantity() <= 0) {
            throw new InvalidRequestException("quantity must be greater than zero");
        }
        if (request.limitPrice().amount().signum() <= 0) {
            throw new InvalidRequestException("limitPrice must be greater than zero");
        }
        if (request.limitPrice().currency() != Currency.USD) {
            throw new InvalidRequestException("limitPrice must be in USD");
        }
        if (request.side() == Side.SELL) {
            long held = portfolio.position(request.symbol()).map(Position::quantity).orElse(0L);
            if (held < request.quantity()) {
                return Optional.of(RejectionReason.INSUFFICIENT_POSITION);
            }
        }
        return Optional.empty();
    }
}
