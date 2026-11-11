package com.tddai.broker.domain.order;

/**
 * Why the exchange did not fill an order it received: there was nothing to fill it with.
 */
public enum KillReason {
    /**
     * The market price is worse than the order's limit.
     */
    LIMIT_NOT_MET,
    /**
     * The exchange does not list this symbol.
     */
    UNKNOWN_SYMBOL
}
