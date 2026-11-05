package br.com.cvc.broker.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Busca por destino. Um item por hotel, com o plano mais barato do período.
 *
 * <p>Valores líquidos: a remuneração do comprador não entra aqui.</p>
 */
public record SearchResponse(String cityCode, LocalDate checkIn, LocalDate checkOut, int adults, int children,
                List<Hit> items) {

    public record Hit(Long hotelId, String hotelName, String cityCode, String cityName, Long roomTypeId,
                    String roomTypeName, Long ratePlanId, String ratePlanName, String board,
                    String cancellationPolicy, BigDecimal totalAmount, String currency, Instant updatedAt,
                    Instant staleAfter, String dataSource) {
    }
}
