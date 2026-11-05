package br.com.cvc.broker.search;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.cvc.broker.api.dto.SearchResponse;
import br.com.cvc.broker.exceptions.BrokerException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Busca por destino: devolve <b>um</b> resultado por hotel, com o plano mais barato do período.
 *
 * <p>Preço é por data, então uma busca sem período não tem preço — e período sem tarifa
 * publicada não entra no resultado, em vez de aparecer com número parcial.</p>
 */
@ApplicationScoped
public class SearchService {

    private final SearchRepository repository;
    private final int freshnessMinutes;
    private final int maxNights;

    public SearchService(final SearchRepository repository,
                    @ConfigProperty(name = "broker.rates.freshness-minutes", defaultValue = "15") final int freshnessMinutes,
                    @ConfigProperty(name = "broker.rates.max-nights", defaultValue = "30") final int maxNights) {
        this.repository = repository;
        this.freshnessMinutes = freshnessMinutes;
        this.maxNights = maxNights;
    }

    public SearchResponse search(final String cityCode, final LocalDate checkIn, final LocalDate checkOut,
                    final int adults, final int children) {
        if (!checkOut.isAfter(checkIn)) {
            throw new BrokerException("invalid-period", 400, "checkOut must be after checkIn");
        }
        final long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > this.maxNights) {
            throw new BrokerException("invalid-period", 400,
                            "Period exceeds the limit of " + this.maxNights + " nights");
        }

        final var adultCount = BigDecimal.valueOf(adults);
        final var childCount = BigDecimal.valueOf(children);

        final Map<Long, List<SearchResponse.Hit>> byHotel = this.repository
                        .offers(cityCode, checkIn, checkOut, nights).stream()
                        .collect(Collectors.groupingBy(SearchRepository.OfferRow::hotelId,
                                        Collectors.mapping(row -> toHit(row, adultCount, childCount),
                                                        Collectors.toList())));

        final var hits = byHotel.values().stream()
                        .map(offers -> offers.stream()
                                        .min(Comparator.comparing(SearchResponse.Hit::totalAmount))
                                        .orElseThrow())
                        .sorted(Comparator.comparing(SearchResponse.Hit::totalAmount))
                        .toList();

        return new SearchResponse(cityCode, checkIn, checkOut, adults, children, hits);
    }

    private SearchResponse.Hit toHit(final SearchRepository.OfferRow row, final BigDecimal adults,
                    final BigDecimal children) {
        // soma das noites (já agregada no banco) × pax, arredondando uma vez
        final var total = row.adultSum().multiply(adults).add(row.childSum().multiply(children))
                        .setScale(2, RoundingMode.HALF_UP);

        return new SearchResponse.Hit(row.hotelId(), row.hotelName(), row.cityCode(), row.cityName(),
                        row.roomTypeId(), row.roomTypeName(), row.ratePlanId(), row.ratePlanName(), row.board(),
                        row.cancellationPolicy(), total, row.currency(), row.updatedAt(),
                        row.updatedAt().plus(Duration.ofMinutes(this.freshnessMinutes)), row.dataSource());
    }

    /** Exposto para o contrato: instante de referência quando não há oferta. */
    public Instant now() {
        return Instant.now();
    }
}
