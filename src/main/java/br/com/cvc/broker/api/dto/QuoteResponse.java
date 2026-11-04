package br.com.cvc.broker.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cotacao. {@code totalAmount} e <b>liquido</b>: remuneracao do comprador nao entra aqui.
 * {@code staleAfter} diz ate quando o comprador pode confiar no numero.
 */
public record QuoteResponse(Long hotelId, Long roomTypeId, Long ratePlanId, LocalDate checkIn, LocalDate checkOut,
                int adults, int children, String currency, BigDecimal totalAmount, List<Night> nights,
                Instant updatedAt, Instant staleAfter, String dataSource) {

    public record Night(LocalDate date, BigDecimal adult, BigDecimal child) {
    }
}
