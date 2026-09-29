package com.tddai.broker.domain.order;

public enum OrderStatus {
    /**
     * Sent to the exchange and executed in full.
     */
    FILLED,
    /**
     * The broker refused to send it (not enough funds or shares).
     */
    REJECTED,
    /**
     * Sent to the exchange, which did not fill it (fill or kill, nothing to fill it with).
     * A normal outcome.
     */
    KILLED
}
