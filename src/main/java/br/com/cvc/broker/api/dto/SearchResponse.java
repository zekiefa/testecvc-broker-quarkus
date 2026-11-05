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
                    Instant staleAfter, String dataSource, List<Night> nights) {
    }

    /**
     * Tarifa líquida por noite do plano vencedor.
     *
     * <p>Existe para o comprador precificar a busca <b>igual</b> à reserva: aplicar a margem
     * noite a noite e somar. Aplicar sobre o total do período daria outro número quando a
     * diária tem dízima — dois preços para o mesmo produto.</p>
     */
    public record Night(java.time.LocalDate date, BigDecimal adult, BigDecimal child) {
    }
}
