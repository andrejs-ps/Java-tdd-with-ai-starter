package com.tddai.exchange.fault;

import com.tddai.exchange.config.ExchangeConfig;

import java.util.random.RandomGenerator;

/**
 * The exchange's internal flakiness: a random chance of a slow response, long enough to trip the
 * broker client's timeout.
 */
public class FaultSimulator {

    private final ExchangeConfig config;
    private final RandomGenerator random;

    public FaultSimulator(ExchangeConfig config, RandomGenerator random) {
        this.config = config;
        this.random = random;
    }

    public void maybeSlowDown() {
        if (config.faultsEnabled() && random.nextDouble() < config.slowChance()) {
            try {
                Thread.sleep(config.slowDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
