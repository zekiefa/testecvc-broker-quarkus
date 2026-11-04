package br.com.cvc.broker.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpos dos endpoints de admin. */
public final class AdminRequests {

    private AdminRequests() {
    }

    public record HotelUpsert(@NotBlank String name, @NotBlank String cityCode, @NotBlank String cityName,
                    BigDecimal latitude, BigDecimal longitude, @Valid @NotNull List<RoomUpsert> roomTypes) {
    }

    public record RoomUpsert(@NotNull Long id, @NotBlank String name, @Min(1) int maxAdults, @Min(0) int maxChildren,
                    @Valid @NotNull List<PlanUpsert> ratePlans) {
    }

    public record PlanUpsert(@NotNull Long id, @NotBlank String name,
                    @NotBlank @Pattern(regexp = "RO|BB|HB|FB") String board,
                    @NotBlank @Pattern(regexp = "FLEXIBLE|NON_REFUNDABLE") String cancellationPolicy) {
    }

    public record RateUpsertBatch(@NotNull Long ratePlanId,
                    @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String currency,
                    @NotNull LocalDate from, @NotNull LocalDate to,
                    @NotNull @DecimalMin("0.00") BigDecimal adultAmount,
                    @NotNull @DecimalMin("0.00") BigDecimal childAmount) {
    }

    public record AvailabilityUpsertBatch(@NotNull LocalDate from, @NotNull LocalDate to,
                    @Min(0) int allotment, boolean stopSell) {
    }
}
