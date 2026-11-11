package com.tddai.exchange;

import com.tddai.exchange.config.ExchangeConfig;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for: POST /orders endpoint, fill or kill at the same prices that are served as quotes
 */
class OrdersTest {

    private static final String UNLISTED_SYMBOL = "NOSYM";

    private Javalin app;

    @BeforeEach
    void start() {
        app = ExchangeApp.start(new ExchangeConfig(0, false, 0.0, 0));
        RestAssured.port = app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private static ValidatableResponse send(String symbol, String side, String quantity, String limitPrice) {
        String body = """
                {
                  "clientOrderId": "client-1",
                  "symbol": "%s",
                  "side": "%s",
                  "quantity": %s,
                  "limitPrice": %s,
                  "timeInForce": "FOK"
                }
                """.formatted(symbol, side, quantity, limitPrice);
        return sendRaw(body);
    }

    private static ValidatableResponse sendRaw(String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/orders")
                .then();
    }

    @Test
    void buyAtOrAboveThePriceIsFilledAtThePrice() {
        send("AAPL", "BUY", "10", "235.00")
                .statusCode(200)
                .body("status", equalTo("FILLED"))
                .body("symbol", equalTo("AAPL"))
                .body("side", equalTo("BUY"))
                .body("quantity", equalTo(10))
                .body("price", equalTo(231.42f))
                .body("orderId", notNullValue())
                .body("clientOrderId", equalTo("client-1"))
                .body("timestamp", notNullValue())
                .body("$", not(hasKey("reason")));
    }

    @Test
    void buyExactlyAtThePriceIsFilled() {
        send("AAPL", "BUY", "1", "231.42")
                .statusCode(200)
                .body("status", equalTo("FILLED"));
    }

    @Test
    void buyBelowThePriceIsKilledWithNothingFilled() {
        send("AAPL", "BUY", "10", "200.00")
                .statusCode(200)
                .body("status", equalTo("KILLED"))
                .body("reason", equalTo("LIMIT_NOT_MET"))
                .body("quantity", equalTo(10))
                .body("$", not(hasKey("price")))
                .body("orderId", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void sellAtOrBelowThePriceIsFilledAtThePrice() {
        send("AAPL", "SELL", "4", "200.00")
                .statusCode(200)
                .body("status", equalTo("FILLED"))
                .body("price", equalTo(231.42f));
    }

    @Test
    void sellAboveThePriceIsKilled() {
        send("AAPL", "SELL", "4", "300.00")
                .statusCode(200)
                .body("status", equalTo("KILLED"))
                .body("reason", equalTo("LIMIT_NOT_MET"));
    }

    @Test
    void anOrderForAnUnlistedSymbolIsKilledBecauseThereIsNothingToFillItWith() {
        send(UNLISTED_SYMBOL, "BUY", "1", "100.00")
                .statusCode(200)
                .body("status", equalTo("KILLED"))
                .body("reason", equalTo("UNKNOWN_SYMBOL"));
    }

    @Test
    void everyOrderGetsItsOwnOrderId() {
        String first = send("AAPL", "BUY", "1", "235").extract().path("orderId");
        String second = send("AAPL", "BUY", "1", "235").extract().path("orderId");

       assertNotEquals(first, second);
    }

    @ParameterizedTest(name = "symbol=''{0}'' side={1} quantity={2} limitPrice={3}")
    @CsvSource({
            "AAPL, HOLD, 1, 100",
            "AAPL, BUY, 0, 100",
            "AAPL, BUY, -2, 100",
            "AAPL, BUY, 1, 0",
            "'', BUY, 1, 100"
    })
    void aMalformedOrderIsRejectedWithA400(String symbol, String side, String quantity, String limitPrice) {
        send(symbol, side, quantity, limitPrice)
                .statusCode(400);
    }

    @Test
    void anOrderWithoutAClientOrderIdIsRejectedWithA400() {
        sendRaw("""
                {"symbol": "AAPL", "side": "BUY", "quantity": 1, "limitPrice": 100}
                """)
                .statusCode(400);
    }

    @Test
    void anOrderWithoutAQuantityIsRejectedWithA400() {
        sendRaw("""
                {"symbol": "AAPL", "side": "BUY", "limitPrice": 100}
                """)
                .statusCode(400);
    }

}
