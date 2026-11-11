package com.tddai.broker.domain.order;

/**
 * How long should the order stay valid
 */
public enum TimeInForce {

    /**
     * FOK (fill or kill): fill the whole quantity immediately at the limit or better, or reject.
     */
    FOK
}
