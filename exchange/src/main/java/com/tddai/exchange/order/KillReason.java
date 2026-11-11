package com.tddai.exchange.order;

/**
 * Why an order was killed: there was nothing to fill it with.
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
