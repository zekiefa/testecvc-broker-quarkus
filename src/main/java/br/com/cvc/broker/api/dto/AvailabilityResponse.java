package br.com.cvc.broker.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Disponibilidade publicada. {@code minAllotment} e o menor valor do periodo — e quanto pode
 * ser vendido para todas as noites. Noite nao publicada aparece com allotment 0 e stopSell:
 * o que nao foi publicado nao pode ser vendido.
 */
public record AvailabilityResponse(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int minAllotment,
                List<Night> nights, Instant updatedAt, Instant staleAfter, String dataSource) {

    public record Night(LocalDate date, int allotment, boolean stopSell) {
    }
}
