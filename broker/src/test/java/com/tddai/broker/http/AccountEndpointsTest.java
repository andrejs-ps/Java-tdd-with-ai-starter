package com.tddai.broker.http;

import com.tddai.broker.support.BrokerIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        String firstAccountId = createAccount("Ada");
        String secondAccountId = createAccount("Grace");

        assertNotEquals(firstAccountId, secondAccountId);
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
    void fetchingAccountDetailsReturnsCorrectData() {
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
}
