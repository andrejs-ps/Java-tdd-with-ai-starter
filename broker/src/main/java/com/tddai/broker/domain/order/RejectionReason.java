package com.tddai.broker.domain.order;

/**
 * Why the broker itself refused to send an order to the exchange.
 */
public enum RejectionReason {
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_POSITION
}
