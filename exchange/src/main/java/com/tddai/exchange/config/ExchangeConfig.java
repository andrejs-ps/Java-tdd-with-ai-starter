package com.tddai.exchange.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Settings for the simulated exchange.
 * Read from exchange.properties on the classpath;
 */
public record ExchangeConfig(int port, boolean faultsEnabled, double slowChance, long slowDelayMs) {

    public static ExchangeConfig load() {
        Properties props = new Properties();
        try (InputStream in = ExchangeConfig.class.getResourceAsStream("/exchange.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ExchangeConfig(
                Integer.parseInt(get(props, "exchange.port", "7070")),
                Boolean.parseBoolean(get(props, "exchange.faults.enabled", "true")),
                Double.parseDouble(get(props, "exchange.faults.slow-chance", "0.2")),
                Long.parseLong(get(props, "exchange.faults.slow-delay-ms", "5000")));
    }

    private static String get(Properties props, String key, String defaultValue) {
        return System.getProperty(key, props.getProperty(key, defaultValue));
    }
}
