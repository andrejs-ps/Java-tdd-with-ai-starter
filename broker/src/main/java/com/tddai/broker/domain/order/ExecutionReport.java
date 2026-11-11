package com.tddai.broker.domain.order;

import com.tddai.broker.domain.Money;

import java.time.Instant;

/**
 * The exchange's answer to an order it received: executed, or killed.
 */
public sealed interface ExecutionReport {

    /**
     * Executed in full at this USD price, at this time.
     */
    record Filled(Money price, Instant timestamp) implements ExecutionReport {
    }

    /**
     * Not executed.
     */
    record Killed(KillReason reason) implements ExecutionReport {
    }
}
