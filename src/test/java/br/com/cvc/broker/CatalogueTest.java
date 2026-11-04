package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

/** Catalogo semeado de Serra Negra: lista, filtro por cidade e detalhe. */
@QuarkusTest
class CatalogueTest {

    private static final String READ_KEY = "read-key-dev";

    @Test
    void listsTheSeededCity() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels?cityCode=serra-negra")
                        .then().statusCode(200)
                        .body("page.total", is(10))
                        .body("items.size()", is(10));
    }

    @Test
    void filtersByAnUnknownCity() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels?cityCode=atlantida")
                        .then().statusCode(200)
                        .body("page.total", is(0))
                        .body("items.size()", is(0));
    }

    @Test
    void paginates() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels?cityCode=serra-negra&limit=3&offset=6")
                        .then().statusCode(200)
                        .body("items.size()", is(3))
                        .body("page.total", is(10))
                        .body("page.offset", is(6));
    }

    @Test
    void detailCarriesRoomsAndRatePlans() {
        final var response = given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels/1")
                        .then().statusCode(200)
                        .body("dataSource", is("SEED"))
                        .body("roomTypes.size()", greaterThan(0))
                        .body("roomTypes[0].ratePlans.size()", greaterThan(0))
                        .extract().response();

        assertFalse(response.jsonPath().getList("roomTypes[0].ratePlans.board").isEmpty());
    }

    @Test
    void unknownHotelIsProblemJson() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels/999999")
                        .then().statusCode(404)
                        .body("type", containsString("/hotel-not-found"));
    }
}
