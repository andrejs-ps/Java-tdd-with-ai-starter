package com.tddai.exchange;

import com.tddai.exchange.config.ExchangeConfig;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for: GET /quotes endpoint
 */
class QuotesTest {

    private static final int PORT = 0;

    /**
     * Well-formed ticker (fits the broker's 1-5 letter rule) that the exchange deliberately does
     * not list.
     */
    private static final String UNLISTED_SYMBOL = "NOSYM";
    private Javalin app;

    @AfterEach
    void stop() {
        if (app != null) {
            app.stop();
        }
    }

    private void startWith(ExchangeConfig config) {
        app = ExchangeApp.start(config);
        RestAssured.port = app.port();
    }

    private static ExchangeConfig noFaults() {
        boolean faultsEnabled = false;
        double slowChance = 0.0;
        long slowDelaMs = 0;
        return new ExchangeConfig(PORT, faultsEnabled, slowChance, slowDelaMs);
    }

    @Test
    void rejectsRequestWithoutSymbols() {
        startWith(noFaults());

        given()
                .when()
                .get("/quotes")
                .then()
                .statusCode(400);
    }

    @Test
    void returnsQuoteForSingleSymbol() {
        startWith(noFaults());

        given().queryParam("symbols", "AAPL")
                .when()
                .get("/quotes")
                .then()
                .statusCode(200)
                .body("AAPL.price", equalTo(231.42f))
                .body("AAPL.timestamp", notNullValue());
    }

    @Test
    void aQuoteStaysWithinTheConfiguredFluctuationOfItsBasePrice() {
        startWith(new ExchangeConfig(PORT, false, 0.0, 0, 0.02));

        for (int i = 0; i < 20; i++) {
            float price = given().queryParam("symbols", "AAPL")
                    .when()
                    .get("/quotes")
                    .then()
                    .statusCode(200)
                    .extract().path("AAPL.price");

            assertTrue(price >= 226.79f && price <= 236.05f, "price out of range: " + price);
        }
    }

    @Test
    void returnsQuotesForSeveralSymbolsInOneCall() {
        startWith(noFaults());

        given().queryParam("symbols", "AAPL,MSFT,IBM")
                .when()
                .get("/quotes")
                .then()
                .statusCode(200)
                .body("$", hasKey("AAPL"))
                .body("$", hasKey("MSFT"))
                .body("$", hasKey("IBM"));
    }

    @Test
    @DisplayName(value = "Every quote in a batch request must have a positive numeric price and non-empty timestamp")
    void quoteNestedValuesArePresent() {
        startWith(noFaults());

        given().queryParam("symbols", "MSFT,IBM")
                .when()
                .get("/quotes")
                .then()
                .statusCode(200)
                .body("MSFT.price", greaterThan(0f))
                .body("MSFT.timestamp", not(emptyOrNullString()))
                .body("IBM.price", greaterThan(0f))
                .body("IBM.timestamp", not(emptyOrNullString()));
    }

    @Test
    void returnsEmptyObjectIfOnlyRequestedSymbolIsUnknown() {
        startWith(noFaults());

        given().queryParam("symbols", UNLISTED_SYMBOL)
                .when()
                .get("/quotes")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    void omitsUnknownSymbols() {
        startWith(noFaults());

        given().queryParam("symbols", "AAPL," + UNLISTED_SYMBOL)
                .when()
                .get("/quotes")
                .then()
                .statusCode(200)
                .body("$", hasKey("AAPL"))
                .body("$", not(hasKey(UNLISTED_SYMBOL)));
    }

    @Test
    void slowResponseFaultDelaysTheResponse() {
        double slowChance = 1.0;
        long slowDelayMs = 300;
        startWith(new ExchangeConfig(PORT, true, slowChance, slowDelayMs));

        long start = System.nanoTime();
        given().queryParam("symbols", "AAPL")
                .when()
                .get("/quotes")
                .then()
                .statusCode(200);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs >= slowDelayMs, "expected a slow response but took " + elapsedMs + " ms");
    }
}
