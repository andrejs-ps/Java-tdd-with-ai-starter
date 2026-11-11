package com.tddai.exchange.order;

import java.math.BigDecimal;

/**
 * The clientOrderId is the sender's own reference; the exchange echoes it back.
 * Prices are USD.
 */
public record OrderRequestDto(
        String clientOrderId,
        String symbol,
        Side side,
        Long quantity,
        BigDecimal limitPrice,
        TimeInForce timeInForce) {
}
