package com.tddai.broker.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;

/**
 * Broker settings.
 * {@link #load()} reads broker.properties from the classpath; any key can be overridden with a
 * system property.
 * Tests construct the record directly.
 */
public record BrokerConfig(int port, String exchangeUrl, Duration exchangeTimeout, StoreImpl storeImpl, String dbUrl) {

    public static BrokerConfig load() {
        Properties props = new Properties();
        try (InputStream in = BrokerConfig.class.getResourceAsStream("/broker.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new BrokerConfig(
                Integer.parseInt(get(props, "broker.port", "7080")),
                get(props, "exchange.url", "http://localhost:7070"),
                Duration.ofMillis(Long.parseLong(get(props, "exchange.timeout-ms", "2000"))),
                StoreImpl.valueOf(get(props, "store.impl", "jdbi").toUpperCase(Locale.ROOT)),
                get(props, "db.url", "jdbc:h2:file:./data/broker"));
    }

    private static String get(Properties props, String key, String defaultValue) {
        return System.getProperty(key, props.getProperty(key, defaultValue));
    }
}
