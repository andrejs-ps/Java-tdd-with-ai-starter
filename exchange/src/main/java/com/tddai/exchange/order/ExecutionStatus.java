package com.tddai.exchange.order;

/**
 * What happened to an order that reached the exchange: executed in full, or killed (nothing
 * executed).
 */
public enum ExecutionStatus {
    FILLED, KILLED
}
