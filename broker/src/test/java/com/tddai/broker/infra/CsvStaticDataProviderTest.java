package com.tddai.broker.infra;

import com.tddai.broker.domain.Currency;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvStaticDataProviderTest {

    private final CsvStaticDataProvider provider = new CsvStaticDataProvider(new StringReader("""
            from,to,rate
            EUR,USD,1.08
            USD,EUR,0.925926
            """));

    @Test
    void readsRatesInBothDirections() {
        assertEquals(new BigDecimal("1.08"), provider.getFxRate(Currency.EUR, Currency.USD));
        assertEquals(new BigDecimal("0.925926"), provider.getFxRate(Currency.USD, Currency.EUR));
    }

    @Test
    void sameCurrencyIsOne() {
        assertEquals(0, BigDecimal.ONE.compareTo(provider.getFxRate(Currency.USD, Currency.USD)));
    }

    @Test
    void anUnlistedPairFails() {
        var onlyOneWay = new CsvStaticDataProvider(new StringReader("from,to,rate\nEUR,USD,1.08\n"));

        assertThrows(IllegalArgumentException.class, () -> onlyOneWay.getFxRate(Currency.USD, Currency.EUR));
    }

    @Test
    void theBundledFileLoads() {
        var bundled = CsvStaticDataProvider.fromClasspath("/fx-rates.csv");

        assertEquals(new BigDecimal("1.08"), bundled.getFxRate(Currency.EUR, Currency.USD));
    }
}
