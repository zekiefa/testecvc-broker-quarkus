package br.com.cvc.broker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import br.com.cvc.broker.api.dto.AvailabilityResponse;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

/** Disponibilidade publicada: minimo do periodo e noite nao publicada como nao vendavel. */
@QuarkusTest
class AvailabilityTest {

    private static final String READ_KEY = "read-key-dev";
    private static final String ADMIN_KEY = "admin-key-dev";

    @Test
    void publishedAvailabilityIsReturnedPerNight() {
        final var checkIn = LocalDate.now().plusDays(60);
        final var response = availability(1L, checkIn, checkIn.plusDays(2));

        assertEquals(2, response.nights().size());
        assertEquals("SEED", response.dataSource());
        assertTrue(response.minAllotment() >= 0);
        assertTrue(response.staleAfter().isAfter(response.updatedAt()));
    }

    @Test
    void unpublishedNightCannotBeSold() {
        final var checkIn = LocalDate.now().plusDays(600);
        final var response = availability(1L, checkIn, checkIn.plusDays(2));

        assertEquals(0, response.minAllotment(), "sem publicacao, nao ha o que vender");
        assertTrue(response.nights().stream().allMatch(AvailabilityResponse.Night::stopSell));
    }

    @Test
    void stopSellDayZeroesThePeriod() {
        // o seed deixa allotment 0 e stop-sell no dia 15 de cada mes
        final var fifteenth = LocalDate.now().plusMonths(2).withDayOfMonth(15);
        final var response = availability(1L, fifteenth.minusDays(1), fifteenth.plusDays(1));

        assertEquals(0, response.minAllotment());
        assertTrue(response.nights().stream()
                        .anyMatch(night -> night.date().equals(fifteenth) && night.stopSell()));
    }

    @Test
    void publishedAllotmentReplacesTheSeed() {
        final var from = LocalDate.now().plusDays(700);
        publish(1L, from, from.plusDays(1), 7);

        final var response = availability(1L, from, from.plusDays(2));
        assertEquals(7, response.minAllotment());
        assertTrue(response.nights().stream().noneMatch(AvailabilityResponse.Night::stopSell));
    }

    @Test
    void invertedPeriodIsRejected() {
        final var checkIn = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/availability?roomTypeId=1&checkIn=" + checkIn + "&checkOut=" + checkIn)
                        .then().statusCode(400)
                        .body("type", containsString("/invalid-period"));
    }

    @Test
    void missingDatesAreRejected() {
        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/availability?roomTypeId=1")
                        .then().statusCode(400)
                        .body("type", containsString("/validation-failed"));
    }

    @Test
    void unknownRoomIsNotFound() {
        final var checkIn = LocalDate.now().plusDays(30);

        given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/availability?roomTypeId=999999&checkIn=" + checkIn
                                        + "&checkOut=" + checkIn.plusDays(1))
                        .then().statusCode(404)
                        .body("type", containsString("/room-not-found"));
    }

    private static AvailabilityResponse availability(final long roomTypeId, final LocalDate checkIn,
                    final LocalDate checkOut) {
        return given().header("X-Api-Key", READ_KEY)
                        .when().get("/v1/availability?roomTypeId=" + roomTypeId + "&checkIn=" + checkIn
                                        + "&checkOut=" + checkOut)
                        .then().statusCode(200)
                        .extract().as(AvailabilityResponse.class);
    }

    private static void publish(final long roomTypeId, final LocalDate from, final LocalDate to,
                    final int allotment) {
        given().header("X-Api-Key", ADMIN_KEY)
                        .contentType("application/json")
                        .body("""
                                        {"from":"%s","to":"%s","allotment":%d,"stopSell":false}
                                        """.formatted(from, to, allotment))
                        .when().put("/admin/rooms/" + roomTypeId + "/availability")
                        .then().statusCode(204);
    }
}
