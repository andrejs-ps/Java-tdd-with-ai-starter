package com.tddai.broker.infra;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Creates the embedded H2 DataSource and applies schema.sql.
 * Shared by both PortfolioStore implementations.
 */
public final class Database {

    private Database() {
    }

    public static DataSource create(String jdbcUrl) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(jdbcUrl);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        applySchema(dataSource);
        return dataSource;
    }

    /**
     * A private in-memory database that lives as long as the JVM; use one per test.
     */
    public static String uniqueInMemoryUrl() {
        return "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private static void applySchema(DataSource dataSource) {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource /schema.sql");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : script.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not apply schema.sql", e);
        }
    }
}
