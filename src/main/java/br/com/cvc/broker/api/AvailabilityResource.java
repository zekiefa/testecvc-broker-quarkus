package br.com.cvc.broker.api;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import br.com.cvc.broker.api.dto.AvailabilityResponse;
import br.com.cvc.broker.catalogue.AvailabilityRepository;
import br.com.cvc.broker.catalogue.CatalogueRepository;
import br.com.cvc.broker.exceptions.BrokerException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Disponibilidade publicada por noite. Informativa: nao bloqueia estoque. */
@Path("/v1/availability")
@Produces(MediaType.APPLICATION_JSON)
public class AvailabilityResource {

    private final CatalogueRepository catalogue;
    private final AvailabilityRepository availability;
    private final int freshnessMinutes;

    public AvailabilityResource(final CatalogueRepository catalogue, final AvailabilityRepository availability,
                    @ConfigProperty(name = "broker.rates.freshness-minutes", defaultValue = "15") final int freshnessMinutes) {
        this.catalogue = catalogue;
        this.availability = availability;
        this.freshnessMinutes = freshnessMinutes;
    }

    @GET
    public AvailabilityResponse availability(@QueryParam("roomTypeId") final long roomTypeId,
                    @QueryParam("checkIn") final LocalDate checkIn,
                    @QueryParam("checkOut") final LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BrokerException("validation-failed", 400, "checkIn and checkOut are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BrokerException("invalid-period", 400, "checkOut must be after checkIn");
        }

        final var room = this.catalogue.room(roomTypeId)
                        .orElseThrow(() -> new BrokerException("room-not-found", 404,
                                        "No room with id " + roomTypeId));
        final var hotel = this.catalogue.hotel(room.hotelId)
                        .orElseThrow(() -> new BrokerException("hotel-not-found", 404,
                                        "No hotel for room " + room.id));

        final Map<LocalDate, br.com.cvc.broker.catalogue.AvailabilityRow> published = this.availability
                        .find(roomTypeId, checkIn, checkOut).stream()
                        .collect(Collectors.toMap(row -> row.stayDate, Function.identity()));

        final long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        final var dates = Stream.iterate(checkIn, date -> date.plusDays(1)).limit(nights).toList();

        final var responseNights = dates.stream()
                        .map(date -> {
                            final var row = published.get(date);
                            return row == null
                                            ? new AvailabilityResponse.Night(date, 0, true)
                                            : new AvailabilityResponse.Night(date, row.allotment, row.stopSell);
                        })
                        .toList();

        final int minAllotment = responseNights.stream().mapToInt(AvailabilityResponse.Night::allotment)
                        .min().orElse(0);
        final Instant updatedAt = published.values().stream().map(row -> row.updatedAt)
                        .max(Comparator.naturalOrder()).orElse(Instant.now());

        return new AvailabilityResponse(room.id, checkIn, checkOut, minAllotment, responseNights, updatedAt,
                        updatedAt.plus(Duration.ofMinutes(this.freshnessMinutes)), hotel.dataSource);
    }
}
