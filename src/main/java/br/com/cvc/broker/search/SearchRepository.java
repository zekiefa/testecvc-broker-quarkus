package br.com.cvc.broker.search;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

/**
 * Busca por destino com o melhor preço do período, em uma consulta.
 *
 * <p>Fazer isso no consumidor exigiria 1 chamada por hotel e outra por quarto/plano — o
 * clássico N+1 disfarçado de integração. Aqui a agregação é do banco: soma as noites por
 * (hotel, quarto, plano) e só considera períodos com <b>todas</b> as noites publicadas
 * ({@code having count(*) = :nights}).</p>
 */
@ApplicationScoped
public class SearchRepository {

    private final EntityManager entityManager;

    public SearchRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Uma linha por hotel/quarto/plano com o período completo publicado. */
    public record OfferRow(long hotelId, String hotelName, String cityCode, String cityName, long roomTypeId,
                    String roomTypeName, long ratePlanId, String ratePlanName, String board,
                    String cancellationPolicy, BigDecimal adultSum, BigDecimal childSum, String currency,
                    Instant updatedAt, String dataSource) {
    }

    /** Uma linha por noite publicada dos quartos informados. */
    public record NightRow(long roomTypeId, long ratePlanId, LocalDate date, BigDecimal adult, BigDecimal child) {
    }

    /** Noites de todos os planos vencedores — uma consulta, filtrando os pares em memoria. */
    @SuppressWarnings("unchecked")
    public List<NightRow> nights(final java.util.List<Long> roomTypeIds, final LocalDate checkIn,
                    final LocalDate checkOut) {
        if (roomTypeIds.isEmpty()) {
            return List.of();
        }
        final List<Object[]> rows = this.entityManager.createNativeQuery(
                        "select room_type_id, rate_plan_id, stay_date, adult_amount, child_amount from rate "
                                        + "where stay_date >= :checkIn and stay_date < :checkOut "
                                        + "and room_type_id = any(:roomIds) order by stay_date")
                        .setParameter("roomIds", roomTypeIds.toArray(new Long[0]))
                        .setParameter("checkIn", checkIn)
                        .setParameter("checkOut", checkOut)
                        .getResultList();

        return rows.stream()
                        .map(row -> new NightRow(((Number) row[0]).longValue(), ((Number) row[1]).longValue(),
                                        (LocalDate) row[2], (BigDecimal) row[3], (BigDecimal) row[4]))
                        .toList();
    }

    @SuppressWarnings("unchecked")
    public List<OfferRow> offers(final String cityCode, final LocalDate checkIn, final LocalDate checkOut,
                    final long nights) {
        final List<Object[]> rows = this.entityManager.createNativeQuery(
                        "select h.id, h.name, h.city_code, h.city_name, rt.id, rt.name, rp.id, rp.name, rp.board, "
                                        + "rp.cancellation_policy, sum(r.adult_amount), sum(r.child_amount), "
                                        + "min(r.currency), max(r.updated_at), h.data_source "
                                        + "from rate r "
                                        + "join room_type rt on rt.id = r.room_type_id "
                                        + "join rate_plan rp on rp.id = r.rate_plan_id "
                                        + "join hotel h on h.id = rt.hotel_id "
                                        + "where h.city_code = :cityCode and r.stay_date >= :checkIn "
                                        + "and r.stay_date < :checkOut "
                                        + "group by h.id, h.name, h.city_code, h.city_name, rt.id, rt.name, "
                                        + "rp.id, rp.name, rp.board, rp.cancellation_policy, h.data_source "
                                        + "having count(*) = :nights "
                                        + "order by h.id, rt.id, rp.id")
                        .setParameter("cityCode", cityCode)
                        .setParameter("checkIn", checkIn)
                        .setParameter("checkOut", checkOut)
                        .setParameter("nights", nights)
                        .getResultList();

        return rows.stream()
                        .map(row -> new OfferRow(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                                        (String) row[3], ((Number) row[4]).longValue(), (String) row[5],
                                        ((Number) row[6]).longValue(), (String) row[7], (String) row[8],
                                        (String) row[9], (BigDecimal) row[10], (BigDecimal) row[11],
                                        (String) row[12], (Instant) row[13], (String) row[14]))
                        .toList();
    }
}
