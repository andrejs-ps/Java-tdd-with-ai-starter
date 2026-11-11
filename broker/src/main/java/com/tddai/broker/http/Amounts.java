package com.tddai.broker.http;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats money amounts for the JSON responses: always two decimals, for example 5000.00.
 * Only the output is rounded; the stored amounts keep their full precision.
 */
final class Amounts {

    private Amounts() {
    }

    static BigDecimal twoDecimals(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
