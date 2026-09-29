package com.tddai.broker.http;

import com.tddai.broker.support.BrokerIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One end-to-end walk through broker, real HttpExchangeClient, stub exchange and H2: fund an
 * account, buy, look at the position.
 * It proves the plumbing; the Course Module 3 integration tests build on this setup.
 */
class OrderWalkingSkeletonTest extends BrokerIntegrationTest {

    @Test
    void fundBuyThenSeeTheValuedPosition() {
        String id = createAccount("Ada");
        deposit(id, "USD", "5000");
        exchange.fillingOrdersAt("231.42");

        post("/accounts/" + id + "/orders", """
                {"symbol": "AAPL", "side": "BUY", "quantity": 10, "limitPrice": 235.00}
                """)
        .statusCode(200)
                .body("status", equalTo("FILLED"))
                .body("symbol", equalTo("AAPL"))
                .body("side", equalTo("BUY"))
                .body("quantity", equalTo(10))
                .body("fillPrice", equalTo(231.42f))
                .body("orderId", notNullValue())
                .body("filledAt", notNullValue());

        assertTrue(exchange.receivedOrders().get(0).contains("clientOrderId"));

        exchange.quoting("AAPL", "240.00");

        given()
                .when()
                .get("/accounts/" + id + "/positions")
                .then()
                .statusCode(200)
                .body("positions", hasSize(1))
                .body("positions[0].symbol", equalTo("AAPL"))
                .body("positions[0].quantity", equalTo(10))
                .body("positions[0].costBasis", equalTo(2314.20f))
                .body("positions[0].marketValue", equalTo(2400.00f))
                .body("positions[0].unrealizedGainLoss", equalTo(85.80f));
    }
}
