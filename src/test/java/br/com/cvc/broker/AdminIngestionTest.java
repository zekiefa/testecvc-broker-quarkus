package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import br.com.cvc.broker.api.dto.QuoteRequest;
import br.com.cvc.broker.api.dto.QuoteResponse;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Ingestao: publicar muda a cotacao e move o frescor. */
@QuarkusTest
class AdminIngestionTest {

    private static final String ADMIN_KEY = "admin-key-dev";
    private static final String READ_KEY = "read-key-dev";

    @Test
    void publishedRateBecomesQuotable() {
        final var from = LocalDate.now().plusDays(500);
        final var to = from.plusDays(3);

        publishRates(1L, 1L, from, to, "1000.00", "500.00", 204);

        final var quote = quote(1L, 1L, 1L, from, from.plusDays(2), 1, 1);

        assertEquals(0, new BigDecimal("3000.00").compareTo(quote.totalAmount()),
                        "duas noites a 1000 + 500 por pax");
        assertEquals(2, quote.nights().size());
    }

    @Test
    void publishingMovesTheFreshnessWindow() {
        final var from = LocalDate.now().plusDays(510);
        publishRates(1L, 1L, from, from.plusDays(2), "800.00", "400.00", 204);

        final var quote = quote(1L, 1L, 1L, from, from.plusDays(1), 1, 0);

        assertTrue(Duration.between(quote.updatedAt(), Instant.now()).toMinutes() < 2,
                        "updatedAt deve ser o instante da publicacao");
        assertTrue(quote.staleAfter().isAfter(quote.updatedAt()), "staleAfter segue o TTL configurado");
    }

    @Test
    void publishingIsIdempotent() {
        final var from = LocalDate.now().plusDays(520);
        publishRates(1L, 1L, from, from, "900.00", "450.00", 204);
        publishRates(1L, 1L, from, from, "910.00", "455.00", 204);

        final var quote = quote(1L, 1L, 1L, from, from.plusDays(1), 1, 0);
        assertEquals(0, new BigDecimal("910.00").compareTo(quote.totalAmount()), "ultima publicacao vence");
    }

    @Test
    void invertedRangeIsRejected() {
        final var from = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", ADMIN_KEY)
                        .contentType(ContentType.JSON)
                        .body("""
                                        {"ratePlanId":1,"currency":"BRL","from":"%s","to":"%s",
                                         "adultAmount":100.00,"childAmount":50.00}
                                        """.formatted(from, from.minusDays(2)))
                        .when().put("/admin/rooms/1/rates")
                        .then().statusCode(400)
                        .body("type", containsString("/invalid-period"));
    }

    @Test
    void publishingForSomeoneElsesPlanIsNotFound() {
        final var from = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", ADMIN_KEY)
                        .contentType(ContentType.JSON)
                        .body("""
                                        {"ratePlanId":1,"currency":"BRL","from":"%s","to":"%s",
                                         "adultAmount":100.00,"childAmount":50.00}
                                        """.formatted(from, from.plusDays(1)))
                        .when().put("/admin/rooms/2/rates")
                        .then().statusCode(404)
                        .body("type", containsString("/rate-plan-not-found"));
    }

    @Test
    void readKeyCannotPublish() {
        final var from = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body("""
                                        {"ratePlanId":1,"currency":"BRL","from":"%s","to":"%s",
                                         "adultAmount":100.00,"childAmount":50.00}
                                        """.formatted(from, from.plusDays(1)))
                        .when().put("/admin/rooms/1/rates")
                        .then().statusCode(403)
                        .body("type", containsString("/insufficient-scope"));
    }

    @Test
    void upsertedHotelIsMarkedAsPushed() {
        final var body = """
                        {"name":"Hotel Publicado","cityCode":"campinas","cityName":"Campinas",
                         "latitude":-22.61,"longitude":-46.70,
                         "roomTypes":[{"id":901,"name":"Chalé","maxAdults":2,"maxChildren":1,
                                       "ratePlans":[{"id":901,"name":"Café — flexível","board":"BB",
                                                     "cancellationPolicy":"FLEXIBLE"}]}]}
                        """;

        given().header("X-Api-Key", ADMIN_KEY)
                        .contentType(ContentType.JSON)
                        .body(body)
                        .when().put("/admin/hotels/900")
                        .then().statusCode(200)
                        .body("dataSource", is("PUSH"));

        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/hotels/900")
                        .then().statusCode(200)
                        .body("name", is("Hotel Publicado"))
                        .body("dataSource", is("PUSH"))
                        .body("roomTypes[0].ratePlans[0].board", is("BB"));
    }

    private static void publishRates(final long roomTypeId, final long ratePlanId, final LocalDate from,
                    final LocalDate to, final String adult, final String child, final int expected) {
        given().header("X-Api-Key", ADMIN_KEY)
                        .contentType(ContentType.JSON)
                        .body("""
                                        {"ratePlanId":%d,"currency":"BRL","from":"%s","to":"%s",
                                         "adultAmount":%s,"childAmount":%s}
                                        """.formatted(ratePlanId, from, to, adult, child))
                        .when().put("/admin/rooms/" + roomTypeId + "/rates")
                        .then().statusCode(expected);
    }

    private static QuoteResponse quote(final long hotelId, final long roomTypeId, final long ratePlanId,
                    final LocalDate checkIn, final LocalDate checkOut, final int adults, final int children) {
        return given().header("X-Api-Key", READ_KEY)
                        .contentType(ContentType.JSON)
                        .body(new QuoteRequest(hotelId, roomTypeId, ratePlanId, checkIn, checkOut, adults, children))
                        .when().post("/v1/quotes")
                        .then().statusCode(200)
                        .extract().as(QuoteResponse.class);
    }
}
