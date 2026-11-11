package com.tddai.exchange.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Price list for a handful of symbols.
 * Each symbol has a base price. With a fluctuation above 0, every quote moves randomly away from
 * the base by up to that fraction (0.02 = 2%), rounded to cents.
 * With a fluctuation of 0 the prices are fixed. Unknown symbols have no quote.
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
    private final double fluctuation;
    private final RandomGenerator random;

    /**
     * A generator with fixed prices.
     */
    public QuoteGenerator(Clock clock) {
        this(clock, 0.0, RandomGenerator.getDefault());
    }

    public QuoteGenerator(Clock clock, double fluctuation, RandomGenerator random) {
        this.clock = clock;
        this.fluctuation = fluctuation;
        this.random = random;
    }

    public Optional<QuoteDto> quoteFor(String symbol) {
        return Optional.ofNullable(PRICES.get(symbol))
                .map(base -> new QuoteDto(fluctuated(base), clock.instant()));
    }

    private BigDecimal fluctuated(BigDecimal base) {
        if (fluctuation == 0.0) {
            return base;
        }
        double factor = 1 + (random.nextDouble() * 2 - 1) * fluctuation;
        return base.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_EVEN);
    }
}
