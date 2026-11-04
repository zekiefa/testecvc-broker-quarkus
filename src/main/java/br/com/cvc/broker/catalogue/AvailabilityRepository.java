package br.com.cvc.broker.catalogue;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

/** Leitura e escrita de disponibilidade por data. */
@ApplicationScoped
public class AvailabilityRepository {

    private final EntityManager entityManager;

    public AvailabilityRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<AvailabilityRow> find(final long roomTypeId, final LocalDate checkIn, final LocalDate checkOut) {
        return this.entityManager.createQuery(
                        "select a from AvailabilityRow a where a.roomTypeId = :roomTypeId "
                                        + "and a.stayDate >= :checkIn and a.stayDate < :checkOut order by a.stayDate",
                        AvailabilityRow.class)
                        .setParameter("roomTypeId", roomTypeId)
                        .setParameter("checkIn", checkIn)
                        .setParameter("checkOut", checkOut)
                        .getResultList();
    }

    /** Upsert do intervalo (inclusive nas duas pontas), renovando {@code updated_at}. */
    public int publish(final long roomTypeId, final LocalDate from, final LocalDate to, final int allotment,
                    final boolean stopSell) {
        return this.entityManager.createNativeQuery(
                        "insert into availability (room_type_id, stay_date, allotment, stop_sell, updated_at) "
                                        + "select :roomTypeId, d::date, :allotment, :stopSell, now() "
                                        + "from generate_series(:from, :to, interval '1 day') d "
                                        + "on conflict (room_type_id, stay_date) do update "
                                        + "set allotment = excluded.allotment, stop_sell = excluded.stop_sell, "
                                        + "updated_at = now()")
                        .setParameter("roomTypeId", roomTypeId)
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .setParameter("allotment", allotment)
                        .setParameter("stopSell", stopSell)
                        .executeUpdate();
    }
}
