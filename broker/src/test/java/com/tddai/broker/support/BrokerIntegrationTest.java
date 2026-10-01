package com.tddai.broker.support;

import com.tddai.broker.BrokerApp;
import com.tddai.broker.config.BrokerConfig;
import com.tddai.broker.infra.Database;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static io.restassured.RestAssured.given;

/**
 * Starts the real broker (in-memory H2, random port) wired to a programmable {@link StubExchange},
 * and points RestAssured at it.
 * Subclasses talk to the broker over real HTTP.
 */
public abstract class BrokerIntegrationTest {

    protected StubExchange exchange;
    private Javalin broker;

    /**
     * Short on purpose, so a stub that delays longer than this produces a timeout.
     */
    protected Duration exchangeTimeout() {
        return Duration.ofMillis(500);
    }

    @BeforeEach
    void startBrokerAndStubExchange() {
        exchange = new StubExchange();
        broker = BrokerApp.start(new BrokerConfig(0, exchange.url(), exchangeTimeout(),
                Database.uniqueInMemoryUrl()));
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = broker.port();
    }

    @AfterEach
    void stopBrokerAndStubExchange() {
        broker.stop();
        exchange.close();
    }

    protected static ValidatableResponse post(String path, String jsonBody) {
        return given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post(path)
                .then();
    }

    protected static String createAccount(String investorName) {
        return post("/accounts", """
                {"investorName": "%s"}
                """.formatted(investorName))
                .statusCode(201)
                .extract().path("accountId");
    }

    protected static void deposit(String accountId, String currency, String amount) {
        postDeposit(accountId, currency, amount)
                .statusCode(200);
    }

    protected static ValidatableResponse postDeposit(String accountId, String currency, String amount) {
        return post("/accounts/" + accountId + "/deposits", """
                {"currency": "%s", "amount": %s}
                """.formatted(currency, amount));
    }

    protected static ValidatableResponse openBalance(String accountId, String currency) {
        return post("/accounts/" + accountId + "/balances", """
                {"currency": "%s"}
                """.formatted(currency));
    }
}
