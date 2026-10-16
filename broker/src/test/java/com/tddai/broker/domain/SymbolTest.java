package com.tddai.broker.domain;

import com.tddai.broker.error.InvalidRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SymbolTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"aapl", "TOOLONG"})
    void symbolMustBeAValidTicker(String symbol) {
        assertThrows(InvalidRequestException.class, () -> Symbol.of(symbol));
    }
}
