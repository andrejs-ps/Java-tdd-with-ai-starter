package com.tddai.broker.infra;

import com.tddai.broker.domain.Currency;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads fx-rates.csv (header: from,to,rate) once, at construction.
 * Everything stays in memory.
 */
public class CsvStaticDataProvider implements StaticDataProvider {

    public record FxRateRow(Currency from, Currency to, BigDecimal rate) {
    }

    private record Pair(Currency from, Currency to) {
    }

    private final Map<Pair, BigDecimal> rates = new HashMap<>();

    public CsvStaticDataProvider(Reader csv) {
        CsvMapper mapper = CsvMapper.builder().build();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        try (var rows = mapper.readerFor(FxRateRow.class).with(schema).<FxRateRow>readValues(csv)) {
            while (rows.hasNext()) {
                FxRateRow row = rows.next();
                rates.put(new Pair(row.from(), row.to()), row.rate());
            }
        }
    }

    public static CsvStaticDataProvider fromClasspath(String resource) {
        try (InputStream in = CsvStaticDataProvider.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + resource);
            }
            return new CsvStaticDataProvider(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public BigDecimal getFxRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = rates.get(new Pair(from, to));
        if (rate == null) {
            throw new IllegalArgumentException("No FX rate from " + from + " to " + to);
        }
        return rate;
    }
}
