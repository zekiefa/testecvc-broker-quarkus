package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Autenticacao por API key e autorizacao por prefixo de caminho. */
@QuarkusTest
class ApiKeyFilterTest {

    private static final String READ_KEY = "read-key-dev";
    private static final String ADMIN_KEY = "admin-key-dev";

    @Test
    void missingKeyIsUnauthorized() {
        given().when().get("/v1/hotels")
                        .then().statusCode(401)
                        .header("Content-Type", containsString("application/problem+json"))
                        .body("type", containsString("/unauthorized"))
                        .body("status", is(401));
    }

    @Test
    void invalidKeyIsUnauthorizedWithoutRevealingAnything() {
        given().header("X-Api-Key", "chute-errado")
                        .when().get("/v1/hotels")
                        .then().statusCode(401)
                        .body("type", containsString("/unauthorized"));
    }

    @Test
    void readKeyCanReadTheCatalogue() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels?cityCode=serra-negra")
                        .then().statusCode(200)
                        .body("page.limit", is(20))
                        .body("page.total", is(10))
                        .body("items[0].dataSource", is("SEED"));
    }

    @Test
    void readKeyCannotReachAdmin() {
        given().header("X-Api-Key", READ_KEY)
                        .when().put("/admin/hotels/1")
                        .then().statusCode(403)
                        .body("type", containsString("/insufficient-scope"));
    }

    @Test
    void adminKeyReachesTheAdminRoutes() {
        final var from = java.time.LocalDate.now().plusDays(800);

        given().header("X-Api-Key", ADMIN_KEY)
                        .contentType(ContentType.JSON)
                        .body("""
                                        {"ratePlanId":1,"currency":"BRL","from":"%s","to":"%s",
                                         "adultAmount":700.00,"childAmount":350.00}
                                        """.formatted(from, from.plusDays(1)))
                        .when().put("/admin/rooms/1/rates")
                        .then().statusCode(204);
    }

    @Test
    void operationsEndpointsDoNotRequireAKey() {
        given().when().get("/q/health").then().statusCode(200);
    }
}
