package com.tddai.broker.http;

import com.tddai.broker.support.BrokerIntegrationTest;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E test with only the Exchange mocked.
 */
class OrderSmokeTest extends BrokerIntegrationTest {

    @Test
    void fundBuyThenSeeTheValuedPosition() {
        String id = fundedAccount();
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

        assertTrue(exchange.receivedOrders().getFirst().contains("clientOrderId"));

        exchange.quoting("AAPL", "240.00");

        getPositions(id)
                .statusCode(200)
                .body("positions", hasSize(1))
                .body("positions[0].symbol", equalTo("AAPL"))
                .body("positions[0].quantity", equalTo(10))
                .body("positions[0].costBasis", equalTo(2314.20f))
                .body("positions[0].marketValue", equalTo(2400.00f))
                .body("positions[0].unrealizedGainLoss", equalTo(85.80f));
    }

    @Test
    void buyThenSellHalf() {
        String id = fundedAccountHoldingTenAapl();

        exchange.fillingOrdersAt("240.00");
        placeOrder(id, "SELL", 5)
                .statusCode(200)
                .body("status", equalTo("FILLED"))
                .body("fillPrice", equalTo(240.00f));

        exchange.quoting("AAPL", "240.00");

        getPositions(id)
                .statusCode(200)
                .body("positions", hasSize(1))
                .body("positions[0].symbol", equalTo("AAPL"))
                .body("positions[0].quantity", equalTo(5))
                .body("positions[0].costBasis", equalTo(1157.10f))
                .body("positions[0].marketValue", equalTo(1200.00f))
                .body("positions[0].unrealizedGainLoss", equalTo(42.90f));

        getAccount(id)
                .statusCode(200)
                .body("balances[0].amount", equalTo(3883.80f));
    }

    @Test
    void buyThenSellAll() {
        String id = fundedAccountHoldingTenAapl();

        exchange.fillingOrdersAt("240.00");
        placeOrder(id, "SELL", 10)
                .statusCode(200)
                .body("status", equalTo("FILLED"))
                .body("fillPrice", equalTo(240.00f));

        getPositions(id)
                .statusCode(200)
                .body("positions", hasSize(0));

        getAccount(id)
                .statusCode(200)
                .body("balances[0].amount", equalTo(5083.80f));
    }

    private String fundedAccount() {
        String id = createAccount("Ada");
        deposit(id, "USD", "5000");
        return id;
    }

    private String fundedAccountHoldingTenAapl() {
        String id = fundedAccount();
        exchange.fillingOrdersAt("231.42");
        placeOrder(id, "BUY", 10)
                .statusCode(200)
                .body("status", equalTo("FILLED"));
        return id;
    }

    private static ValidatableResponse placeOrder(String accountId, String side, int quantity) {
        return post("/accounts/" + accountId + "/orders", """
                {"symbol": "AAPL", "side": "%s", "quantity": %d, "limitPrice": 235.00}
                """.formatted(side, quantity));
    }

    private static ValidatableResponse getPositions(String accountId) {
        return given()
                .when()
                .get("/accounts/" + accountId + "/positions")
                .then();
    }

    private static ValidatableResponse getAccount(String accountId) {
        return given()
                .when()
                .get("/accounts/" + accountId)
                .then();
    }
}
