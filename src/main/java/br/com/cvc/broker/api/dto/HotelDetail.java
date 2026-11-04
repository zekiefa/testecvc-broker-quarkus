package br.com.cvc.broker.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Hotel com quartos e regimes, como o comprador precisa para cotar. */
public record HotelDetail(Long id, String name, String cityCode, String cityName, BigDecimal latitude,
                BigDecimal longitude, String dataSource, Instant updatedAt, List<Room> roomTypes) {

    public record Room(Long id, String name, int maxAdults, int maxChildren, List<Plan> ratePlans) {
    }

    public record Plan(Long id, String name, String board, String cancellationPolicy) {
    }
}
