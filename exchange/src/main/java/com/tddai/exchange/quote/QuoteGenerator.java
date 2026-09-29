package com.tddai.exchange.quote;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed price list for a handful of symbols.
 * Deterministic on purpose; unknown symbols have no quote.
 */
public class QuoteGenerator {

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "AAPL", new BigDecimal("231.42"),
            "MSFT", new BigDecimal("415.10"),
            "IBM", new BigDecimal("190.55"),
            "KO", new BigDecimal("62.30"),
            "JPM", new BigDecimal("205.75"),
            "XOM", new BigDecimal("112.40"));

    private final Clock clock;

    public QuoteGenerator(Clock clock) {
        this.clock = clock;
    }

    public Optional<QuoteDto> quoteFor(String symbol) {
        return Optional.ofNullable(PRICES.get(symbol))
                .map(price -> new QuoteDto(price, clock.instant()));
    }
}
