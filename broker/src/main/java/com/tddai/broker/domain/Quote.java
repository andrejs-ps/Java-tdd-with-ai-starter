package com.tddai.broker.domain;

import java.time.Instant;

public record Quote(Symbol symbol, Money price, Instant timestamp) {
}
