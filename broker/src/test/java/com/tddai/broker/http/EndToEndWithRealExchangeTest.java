package com.tddai.broker.http;

import com.tddai.broker.BrokerApp;
import com.tddai.broker.config.BrokerConfig;
import com.tddai.broker.config.StoreImpl;
import com.tddai.broker.infra.Database;
import com.tddai.exchange.ExchangeApp;
import com.tddai.exchange.config.ExchangeConfig;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Happy paths against the REAL exchange Maven Module, with its random slowness switched off (the
 * equivalent of exchange.faults.enabled=false) so the result is deterministic.
 */
class EndToEndWithRealExchangeTest {

    private Javalin exchange;
    private Javalin broker;

    @BeforeEach
    void start() {
        exchange = ExchangeApp.start(new ExchangeConfig(0, false, 0.0, 0));
        broker = BrokerApp.start(new BrokerConfig(0, "http://localhost:" + exchange.port(),
                Duration.ofSeconds(2), StoreImpl.JDBI, Database.uniqueInMemoryUrl()));
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = broker.port();
    }

    @AfterEach
    void stop() {
        broker.stop();
        exchange.stop();
    }

    private static ValidatableResponse post(String path, String json) {
        return given()
                .contentType(ContentType.JSON)
                .body(json)
                .when()
                .post(path)
                .then();
    }

    private static String fundedAccount() {
        String id = post("/accounts", """
                {"investorName": "Ada"}
                """)
                .statusCode(201)
                .extract().path("accountId");
        post("/accounts/" + id + "/deposits", """
                {"currency": "USD", "amount": 5000}
                """)
                .statusCode(200);
        return id;
    }

    @Test
    void buyAtTheRealQuoteThenSellPartOfItAndSeeTheValuedPosition() {
        String id = fundedAccount();

        post("/accounts/" + id + "/orders", """
                {"symbol": "AAPL", "side": "BUY", "quantity": 10, "limitPrice": 235.00}
                """)
        .statusCode(200)
                .body("fillPrice", equalTo(231.42f));

        post("/accounts/" + id + "/orders", """
                {"symbol": "AAPL", "side": "SELL", "quantity": 4, "limitPrice": 200.00}
                """)
        .statusCode(200)
                .body("fillPrice", equalTo(231.42f));

        given()
                .when()
                .get("/accounts/" + id + "/positions")
                .then()
                .statusCode(200)
                .body("positions[0].quantity", equalTo(6))
                .body("positions[0].costBasis", equalTo(1388.52f))
                .body("positions[0].marketValue", equalTo(1388.52f))
                .body("positions[0].unrealizedGainLoss", equalTo(0.0f));

        given()
                .when()
                .get("/accounts/" + id)
                .then()
                .body("balances[0].amount", equalTo(3611.48f));
    }

    @Test
    void aBuyBelowTheMarketIsKilledAndTheAccountIsUntouched() {
        String id = fundedAccount();

        post("/accounts/" + id + "/orders", """
                {"symbol": "AAPL", "side": "BUY", "quantity": 10, "limitPrice": 200.00}
                """)
        .statusCode(200)
                .body("status", equalTo("KILLED"))
                .body("reason", equalTo("LIMIT_NOT_MET"));

        given()
                .when()
                .get("/accounts/" + id + "/positions")
                .then()
                .body("positions.size()", equalTo(0));
        given()
                .when()
                .get("/accounts/" + id)
                .then()
                .body("balances[0].amount", equalTo(5000.0f));
    }
}
