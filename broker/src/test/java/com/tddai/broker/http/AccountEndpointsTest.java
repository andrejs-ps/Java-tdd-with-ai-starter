package com.tddai.broker.http;

import com.tddai.broker.support.BrokerIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Pre-built account set-up endpoints; not part of what the course teaches.
 */
class AccountEndpointsTest extends BrokerIntegrationTest {

    @Test
    void createsAnAccountWithAnEmptyUsdBalance() {
        post("/accounts", """
                {"investorName": "Ada"}
                """)
        .statusCode(201)
                .body("accountId", notNullValue())
                .body("investorName", equalTo("Ada"))
                .body("balances", hasSize(1))
                .body("balances[0].currency", equalTo("USD"))
                .body("balances[0].amount", equalTo(0.0f));
    }

    @Test
    void everyInvestorGetsTheirOwnAccount() {
        String first = createAccount("Ada");
        String second = createAccount("Grace");

        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
    }

    @Test
    void readsBackTheAccount() {
        String id = createAccount("Ada");

        given()
                .when()
                .get("/accounts/" + id)
                .then()
                .statusCode(200)
                .body("accountId", equalTo(id))
                .body("investorName", equalTo("Ada"));
    }

    @Test
    void anUnknownAccountIsNotFound() {
        given()
                .when()
                .get("/accounts/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void aMalformedAccountIdIsNotFound() {
        given()
                .when()
                .get("/accounts/not-a-uuid")
                .then()
                .statusCode(404);
    }

    @Test
    void aBlankInvestorNameIsRejected() {
        post("/accounts", """
                {"investorName": "  "}
                """)
                .statusCode(400);
    }

    @Test
    void aBodyThatIsNotCompleteJsonIsRejected() {
        post("/accounts", """
                {"investorName":
                """)
                .statusCode(400);
    }

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
    void opensAnEurBalanceAndThenDepositsIntoIt() {
        String id = createAccount("Ada");

        openBalance(id, "EUR")
                .statusCode(201)
                .body("balances", hasSize(2));

        postDeposit(id, "EUR", "200")
                .statusCode(200)
                .body("balances.find { it.currency == 'EUR' }.amount", equalTo(200.0f));
    }

    @Test
    void opensEurOnlyOnce() {
        String id = createAccount("Ada");
        openBalance(id, "EUR")
                .statusCode(201);

        openBalance(id, "EUR")
                .statusCode(409);
    }

    @Test
    void usdIsAlreadyOpenSoOpeningItConflicts() {
        String id = createAccount("Ada");

        openBalance(id, "USD")
                .statusCode(409);
    }

    @Test
    void noOtherCurrencyCanBeOpened() {
        String id = createAccount("Ada");

        openBalance(id, "GBP")
                .statusCode(400);
    }

    @Test
    void openingABalanceOnAnUnknownAccountIsNotFound() {
        openBalance(UUID.randomUUID().toString(), "EUR")
                .statusCode(404);
    }
}
