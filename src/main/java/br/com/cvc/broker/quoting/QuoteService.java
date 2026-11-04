package br.com.cvc.broker.quoting;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import br.com.cvc.broker.api.dto.QuoteRequest;
import br.com.cvc.broker.api.dto.QuoteResponse;
import br.com.cvc.broker.catalogue.CatalogueRepository;
import br.com.cvc.broker.catalogue.RateRepository;
import br.com.cvc.broker.catalogue.RateRepository.NightRateRow;
import br.com.cvc.broker.exceptions.BrokerException;
import br.com.cvc.broker.exceptions.IncompleteRatePeriodException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Caso de uso da cotacao: valida o periodo, resolve catalogo, le as tarifas de todas as noites
 * e devolve o detalhamento com frescor.
 *
 * <p>Duas regras que nao podem afrouxar: periodo com noite faltando e <b>409</b> (nunca total
 * parcial) e capacidade do quarto e limite do vendedor, nao sugestao.</p>
 */
@ApplicationScoped
public class QuoteService {

    private final CatalogueRepository catalogue;
    private final RateRepository rates;
    private final int freshnessMinutes;
    private final int maxNights;

    public QuoteService(final CatalogueRepository catalogue, final RateRepository rates,
                    @ConfigProperty(name = "broker.rates.freshness-minutes", defaultValue = "15") final int freshnessMinutes,
                    @ConfigProperty(name = "broker.rates.max-nights", defaultValue = "30") final int maxNights) {
        this.catalogue = catalogue;
        this.rates = rates;
        this.freshnessMinutes = freshnessMinutes;
        this.maxNights = maxNights;
    }

    public QuoteResponse quote(final QuoteRequest request) {
        final long nights = validatePeriod(request);

        final var hotel = this.catalogue.hotel(request.hotelId())
                        .orElseThrow(() -> new BrokerException("hotel-not-found", 404,
                                        "No hotel with id " + request.hotelId()));
        final var room = this.catalogue.room(request.roomTypeId())
                        .orElseThrow(() -> new BrokerException("room-not-found", 404,
                                        "No room with id " + request.roomTypeId()));
        if (!room.hotelId.equals(hotel.id)) {
            throw new BrokerException("room-not-found", 404,
                            "Room " + room.id + " does not belong to hotel " + hotel.id);
        }

        final var plan = this.catalogue.plan(request.ratePlanId())
                        .orElseThrow(() -> new BrokerException("rate-plan-not-found", 404,
                                        "No rate plan with id " + request.ratePlanId()));
        if (!plan.roomTypeId.equals(room.id)) {
            throw new BrokerException("rate-plan-not-found", 404,
                            "Rate plan " + plan.id + " does not belong to room " + room.id);
        }

        if (request.adults() > room.maxAdults) {
            throw new BrokerException("guest-limit-exceeded", 400,
                            "Room supports at most " + room.maxAdults + " adult(s)");
        }
        if (request.children() > room.maxChildren) {
            throw new BrokerException("guest-limit-exceeded", 400,
                            "Room supports at most " + room.maxChildren + " child(ren)");
        }

        final var rows = this.rates.find(room.id, plan.id, request.checkIn(), request.checkOut());
        requireCompletePeriod(rows, request.checkIn(), request.checkOut());

        final var total = Quoting.total(rows, request.adults(), request.children());
        final var updatedAt = rows.stream().map(NightRateRow::updatedAt).max(Comparator.naturalOrder())
                        .orElse(Instant.now());

        return new QuoteResponse(hotel.id, room.id, plan.id, request.checkIn(), request.checkOut(),
                        request.adults(), request.children(), rows.get(0).currency(), total,
                        rows.stream().map(row -> new QuoteResponse.Night(row.date(), row.adult(), row.child())).toList(),
                        updatedAt, updatedAt.plus(Duration.ofMinutes(this.freshnessMinutes)), hotel.dataSource);
    }

    private long validatePeriod(final QuoteRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new BrokerException("invalid-period", 400, "checkOut must be after checkIn");
        }
        final long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights > this.maxNights) {
            throw new BrokerException("invalid-period", 400,
                            "Period exceeds the limit of " + this.maxNights + " nights");
        }
        return nights;
    }

    private static void requireCompletePeriod(final List<NightRateRow> rows, final LocalDate checkIn,
                    final LocalDate checkOut) {
        final var published = rows.stream().map(NightRateRow::date).collect(Collectors.toSet());
        final var missing = datesOf(checkIn, checkOut).filter(date -> !published.contains(date)).toList();

        if (!missing.isEmpty()) {
            throw new IncompleteRatePeriodException(missing);
        }
    }

    private static Stream<LocalDate> datesOf(final LocalDate checkIn, final LocalDate checkOut) {
        final long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return Stream.iterate(checkIn, date -> date.plusDays(1)).limit(nights);
    }
}
