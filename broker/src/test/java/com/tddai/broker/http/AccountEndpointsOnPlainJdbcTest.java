package com.tddai.broker.http;

import com.tddai.broker.config.StoreImpl;
import com.tddai.broker.support.BrokerIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * The same account flow on the plain-JDBC store, showing the two PortfolioStore implementations are
 * interchangeable.
 */
class AccountEndpointsOnPlainJdbcTest extends BrokerIntegrationTest {

    @Override
    protected StoreImpl storeImpl() {
        return StoreImpl.JDBC;
    }

    @Test
    void createsOpensAndDeposits() {
        String id = createAccount("Ada");
        openBalance(id, "EUR")
                .statusCode(201);
        deposit(id, "EUR", "75.5");

        given()
                .when()
                .get("/accounts/" + id)
                .then()
                .statusCode(200)
                .body("balances.find { it.currency == 'EUR' }.amount", equalTo(75.5f));
    }
}
