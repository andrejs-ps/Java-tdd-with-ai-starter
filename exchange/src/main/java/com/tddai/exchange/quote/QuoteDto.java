package com.tddai.exchange.quote;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire shape of one quote: {"price":231.42,"timestamp":"2026-01-02T15:04:05Z"}.
 * Always USD.
 */
public record QuoteDto(BigDecimal price, Instant timestamp) {
}
