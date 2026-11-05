package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.cvc.broker.api.dto.SearchResponse;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

/** Busca por destino: um item por hotel, melhor preço do período, ordenado. */
@QuarkusTest
class SearchTest {

    private static final String READ_KEY = "read-key-dev";

    @Test
    void returnsOneItemPerHotelOrderedByPrice() {
        final var response = search("serra-negra", LocalDate.now().plusDays(90), 3, 1, 1);

        assertEquals(10, response.items().size(), "dez hoteis semeados, um item cada");
        assertTrue(response.items().stream().allMatch(hit -> hit.totalAmount().compareTo(BigDecimal.ZERO) > 0));

        final List<BigDecimal> prices = response.items().stream().map(SearchResponse.Hit::totalAmount).toList();
        final var sorted = prices.stream().sorted().toList();
        assertEquals(sorted, prices, "resultado deve vir do mais barato ao mais caro");

        assertTrue(response.items().stream().allMatch(hit -> "SEED".equals(hit.dataSource())));
    }

    @Test
    void periodWithoutRatesReturnsNoOffer() {
        // longe da janela semeada (400 dias) e de qualquer publicacao feita por outros testes
        final var response = search("serra-negra", LocalDate.now().plusDays(2000), 2, 1, 1);
        assertTrue(response.items().isEmpty(), "periodo fora da janela publicada nao oferta nada");
    }

    @Test
    void unknownCityReturnsEmptyList() {
        final var response = search("atlantida", LocalDate.now().plusDays(90), 2, 1, 1);
        assertTrue(response.items().isEmpty());
    }

    @Test
    void invertedPeriodIsRejected() {
        final var checkIn = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/search?cityCode=serra-negra&checkIn=" + checkIn
                                        + "&checkOut=" + checkIn + "&adults=1&children=1")
                        .then().statusCode(400)
                        .body("type", containsString("/invalid-period"));
    }

    @Test
    void missingParametersAreRejected() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/search?cityCode=serra-negra")
                        .then().statusCode(400)
                        .body("type", containsString("/validation-failed"));
    }

    @Test
    void requiresApiKey() {
        given().when().get("/v1/search?cityCode=serra-negra&checkIn=2027-04-10&checkOut=2027-04-13&adults=1&children=1")
                        .then().statusCode(401);
    }

    private static SearchResponse search(final String city, final LocalDate checkIn, final int nights,
                    final int adults, final int children) {
        return given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/search?cityCode=" + city + "&checkIn=" + checkIn
                                        + "&checkOut=" + checkIn.plusDays(nights) + "&adults=" + adults
                                        + "&children=" + children)
                        .then().statusCode(200)
                        .body("items.size()", greaterThan(-1))
                        .extract().as(SearchResponse.class);
    }
}
