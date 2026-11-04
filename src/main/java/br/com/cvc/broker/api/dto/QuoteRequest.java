package br.com.cvc.broker.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Corpo de {@code POST /v1/quotes}. A validacao estrutural e por anotacao (400). */
public record QuoteRequest(@NotNull Long hotelId,
                @NotNull Long roomTypeId,
                @NotNull Long ratePlanId,
                @NotNull LocalDate checkIn,
                @NotNull LocalDate checkOut,
                @NotNull @Min(1) Integer adults,
                @NotNull @Min(0) @Max(10) Integer children) {
}
