package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

/** Todo erro sai em RFC 7807, com tipo estavel e traceId ligando resposta e log. */
@QuarkusTest
class ProblemDetailsTest {

    private static final String READ_KEY = "read-key-dev";

    @Test
    void unknownRouteIsProblemJson() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/nao-existe")
                        .then().statusCode(404)
                        .header("Content-Type", containsString("application/problem+json"))
                        .body("status", is(404));
    }

    @Test
    void missingHotelCarriesItsOwnType() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels/999")
                        .then().statusCode(404)
                        .body("type", containsString("/hotel-not-found"))
                        .body("detail", containsString("999"));
    }

    @Test
    void errorCarriesTheSameTraceIdAsTheHeader() {
        final var response = given().header("X-Api-Key", READ_KEY)
                        .header("X-Request-Id", "trace-do-broker")
                        .when().get("/v1/hotels/999")
                        .then().statusCode(404)
                        .extract().response();

        assertEquals("trace-do-broker", response.header("X-Request-Id"));
        assertEquals("trace-do-broker", response.jsonPath().getString("traceId"));
    }

    @Test
    void generatesATraceIdWhenAbsent() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels")
                        .then().statusCode(200)
                        .header("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }
}
