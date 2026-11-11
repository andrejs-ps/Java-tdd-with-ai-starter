package com.tddai.broker.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrokerConfigTest {

    private static BrokerConfig configWithFee(String flatFeeUsd) {
        return new BrokerConfig(0, "http://localhost:7070", Duration.ofSeconds(2), "jdbc:h2:mem:test",
                new BigDecimal(flatFeeUsd));
    }

    @Test
    void aFeeIsKeptAsGiven() {
        var config = configWithFee("1.00");

        var expected = new BigDecimal("1.00");
        var actual = config.flatFeeUsd();
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-1.00"})
    void aNegativeFeeMeansNoFee(String negativeFee) {
        var config = configWithFee(negativeFee);

        var expected = BigDecimal.ZERO;
        var actual = config.flatFeeUsd();
        assertEquals(expected, actual);
    }
}
