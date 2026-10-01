package com.tddai.broker.http;

import com.tddai.broker.support.BrokerIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class DepositEndpointsTest extends BrokerIntegrationTest {

    @Test
    void depositsIntoTheUsdBalance() {
        String id = createAccount("Ada");

        postDeposit(id, "USD", "1500.50")
                .statusCode(200)
                .body("balances[0].currency", equalTo("USD"))
                .body("balances[0].amount", equalTo(1500.50f));
    }

    @Test
    void depositsAccumulate() {
        String id = createAccount("Ada");
        deposit(id, "USD", "100");
        deposit(id, "USD", "250.25");

        given()
                .when()
                .get("/accounts/" + id)
                .then()
                .body("balances[0].amount", equalTo(350.25f));
    }

    @Test
    void rejectsANonPositiveDeposit() {
        String id = createAccount("Ada");

        postDeposit(id, "USD", "0")
                .statusCode(400);
        postDeposit(id, "USD", "-5")
                .statusCode(400);
    }

    @Test
    void cannotDepositIntoAnEurBalanceThatIsNotOpen() {
        String id = createAccount("Ada");

        postDeposit(id, "EUR", "100")
                .statusCode(404);
    }

    @Test
    void cannotDepositIntoAnUnknownAccount() {
        postDeposit(UUID.randomUUID().toString(), "USD", "100")
                .statusCode(404);
    }

    @Test
    void depositsIntoAnEurBalanceOnceItIsOpen() {
        String id = createAccount("Ada");
        openBalance(id, "EUR");

        postDeposit(id, "EUR", "200")
                .statusCode(200)
                .body("balances.find { it.currency == 'EUR' }.amount", equalTo(200.0f));
    }
}
