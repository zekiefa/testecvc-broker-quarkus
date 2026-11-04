package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.cvc.broker.api.dto.QuoteRequest;
import br.com.cvc.broker.api.dto.QuoteResponse;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Cotacao: soma das diarias, frescor explicito e periodo incompleto como erro. */
@QuarkusTest
class QuoteTest {

    private static final String READ_KEY = "read-key-dev";
    private static final String QUOTES = "/v1/quotes";

    @Test
    void quotesTheWholePeriod() {
        final var checkIn = LocalDate.now().plusDays(30);
        final var response = quote(hotel1(checkIn, 3, 1, 1));

        assertEquals(3, response.nights().size());
        assertEquals("BRL", response.currency());
        assertEquals("SEED", response.dataSource());
        assertNotNull(response.updatedAt());
        assertTrue(response.staleAfter().isAfter(response.updatedAt()), "staleAfter deve ser depois de updatedAt");

        final var expected = response.nights().stream()
                        .map(night -> night.adult().multiply(BigDecimal.ONE).add(night.child().multiply(BigDecimal.ONE)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, java.math.RoundingMode.HALF_UP);

        assertEquals(0, expected.compareTo(response.totalAmount()),
                        "total deve ser a soma das diarias do periodo");
    }

    @Test
    void chargesPerGuest() {
        final var checkIn = LocalDate.now().plusDays(40);
        final var oneAdult = quote(hotel1(checkIn, 2, 1, 0));
        final var twoAdults = quote(hotel1(checkIn, 2, 2, 0));

        assertTrue(twoAdults.totalAmount().compareTo(oneAdult.totalAmount()) > 0,
                        "dois adultos custam mais que um");
    }

    @Test
    void periodWithoutPublishedRateIsConflict() {
        // longe da janela semeada (400 dias) e de qualquer publicacao dos testes
        final var checkIn = LocalDate.now().plusDays(900);

        final var response = given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body(hotel1(checkIn, 2, 1, 1))
                        .when().post(QUOTES)
                        .then().statusCode(409)
                        .body("type", containsString("/incomplete-rate-period"))
                        .extract().response();

        final List<String> missing = response.jsonPath().getList("missingDates");
        assertEquals(2, missing.size(), "as duas noites faltantes devem ser listadas");
    }

    @Test
    void invertedPeriodIsBadRequest() {
        final var checkIn = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body(hotel1(checkIn, 0, 1, 1))
                        .when().post(QUOTES)
                        .then().statusCode(400)
                        .body("type", containsString("/invalid-period"));
    }

    @Test
    void guestLimitIsEnforced() {
        final var checkIn = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body(hotel1(checkIn, 2, 9, 0))
                        .when().post(QUOTES)
                        .then().statusCode(400)
                        .body("type", containsString("/guest-limit-exceeded"));
    }

    @Test
    void unknownHotelIsNotFound() {
        final var request = new QuoteRequest(999999L, 1L, 1L, LocalDate.now().plusDays(30),
                        LocalDate.now().plusDays(32), 1, 0);

        given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when().post(QUOTES)
                        .then().statusCode(404)
                        .body("type", containsString("/hotel-not-found"));
    }

    @Test
    void structuralValidationIsNamed() {
        given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body("{\"hotelId\": 1}")
                        .when().post(QUOTES)
                        .then().statusCode(400)
                        .body("type", containsString("/validation-failed"));
    }

    private QuoteResponse quote(final QuoteRequest request) {
        return given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body(request)
                        .when().post(QUOTES)
                        .then().statusCode(200)
                        .extract().as(QuoteResponse.class);
    }

    private static QuoteRequest hotel1(final LocalDate checkIn, final int nights, final int adults,
                    final int children) {
        return new QuoteRequest(1L, 1L, 1L, checkIn, checkIn.plusDays(nights), adults, children);
    }
}
