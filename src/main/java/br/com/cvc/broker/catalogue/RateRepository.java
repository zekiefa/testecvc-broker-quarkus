package br.com.cvc.broker.catalogue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Tarifas por data de estadia. Consulta em SQL nativo porque a chave e composta
 * (quarto, plano, data) e o que interessa e a janela do periodo, nao a entidade.
 */
@ApplicationScoped
public class RateRepository {

    private final EntityManager entityManager;

    public RateRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Noite publicada: data, tarifas por pax, moeda e quando foi atualizada. */
    public record NightRateRow(LocalDate date, BigDecimal adult, BigDecimal child, String currency,
                    Instant updatedAt) {
    }

    /** Upsert do intervalo (inclusive), renovando {@code updated_at}: e isso que move o frescor. */
    public int publish(final long roomTypeId, final long ratePlanId, final LocalDate from, final LocalDate to,
                    final BigDecimal adultAmount, final BigDecimal childAmount, final String currency) {
        return this.entityManager.createNativeQuery(
                        "insert into rate (room_type_id, rate_plan_id, stay_date, adult_amount, child_amount, currency, updated_at) "
                                        + "select :roomTypeId, :ratePlanId, d::date, :adult, :child, :currency, now() "
                                        + "from generate_series(:from, :to, interval '1 day') d "
                                        + "on conflict (room_type_id, rate_plan_id, stay_date) do update "
                                        + "set adult_amount = excluded.adult_amount, child_amount = excluded.child_amount, "
                                        + "currency = excluded.currency, updated_at = now()")
                        .setParameter("roomTypeId", roomTypeId)
                        .setParameter("ratePlanId", ratePlanId)
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .setParameter("adult", adultAmount)
                        .setParameter("child", childAmount)
                        .setParameter("currency", currency)
                        .executeUpdate();
    }

    public List<NightRateRow> find(final long roomTypeId, final long ratePlanId,
                    final LocalDate checkIn, final LocalDate checkOut) {
        final TypedQuery<Object[]> query = this.entityManager.createQuery(
                        "select r.stayDate, r.adultAmount, r.childAmount, r.currency, r.updatedAt "
                                        + "from RateRow r where r.roomTypeId = :roomTypeId and r.ratePlanId = :ratePlanId "
                                        + "and r.stayDate >= :checkIn and r.stayDate < :checkOut order by r.stayDate",
                        Object[].class);
        query.setParameter("roomTypeId", roomTypeId);
        query.setParameter("ratePlanId", ratePlanId);
        query.setParameter("checkIn", checkIn);
        query.setParameter("checkOut", checkOut);

        return query.getResultList().stream()
                        .map(row -> new NightRateRow((LocalDate) row[0], (BigDecimal) row[1], (BigDecimal) row[2],
                                        ((String) row[3]).trim(), (Instant) row[4]))
                        .toList();
    }
}
