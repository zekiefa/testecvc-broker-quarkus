package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

/**
 * O contrato e servido como esta escrito em {@code openapi/broker-v1.yaml} — nada e gerado
 * por anotacao. Se alguem remover uma rota do arquivo, este teste cai.
 */
@QuarkusTest
class OpenApiContractTest {

    @Test
    void servesTheHandWrittenContract() {
        final var body = given().when().get("/q/openapi")
                        .then().statusCode(200)
                        .extract().asString();

        for (final var path : new String[] { "/v1/hotels", "/v1/search", "/v1/quotes", "/v1/availability",
                        "/admin/hotels/{hotelId}", "/admin/rooms/{roomTypeId}/rates" }) {
            if (!body.contains(path)) {
                throw new AssertionError("contrato servido nao contem " + path);
            }
        }
    }

    @Test
    void contractDeclaresFreshnessAndDataSource() {
        given().when().get("/q/openapi")
                        .then().statusCode(200)
                        .body(containsString("staleAfter"))
                        .body(containsString("dataSource"));
    }
}
