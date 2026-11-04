package br.com.cvc.broker.catalogue;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

/**
 * Upsert de catalogo por id explicito.
 *
 * <p>O contrato de admin recebe o id do item, e {@code GenerationType.IDENTITY} ignora id
 * atribuido — entao aqui a escrita e SQL nativo com {@code on conflict}. Depois de cada
 * escrita a sequence e reposicionada, senao o proximo id gerado pelo banco colidiria.</p>
 */
@ApplicationScoped
public class IngestionRepository {

    private final EntityManager entityManager;

    public IngestionRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void upsertHotel(final long id, final String name, final String cityCode, final String cityName,
                    final BigDecimal latitude, final BigDecimal longitude) {
        this.entityManager.createNativeQuery(
                        "insert into hotel (id, name, city_code, city_name, latitude, longitude, data_source, updated_at) "
                                        + "values (:id, :name, :cityCode, :cityName, :lat, :lng, 'PUSH', now()) "
                                        + "on conflict (id) do update set name = excluded.name, "
                                        + "city_code = excluded.city_code, city_name = excluded.city_name, "
                                        + "latitude = excluded.latitude, longitude = excluded.longitude, "
                                        + "data_source = 'PUSH', updated_at = now()")
                        .setParameter("id", id)
                        .setParameter("name", name)
                        .setParameter("cityCode", cityCode)
                        .setParameter("cityName", cityName)
                        .setParameter("lat", latitude)
                        .setParameter("lng", longitude)
                        .executeUpdate();
        syncHotelSequence();
    }

    public Long findHotelId(final long id) {
        final var result = this.entityManager.createNativeQuery("select id from hotel where id = :id")
                        .setParameter("id", id)
                        .getResultList();
        return result.isEmpty() ? null : ((Number) result.get(0)).longValue();
    }

    public Long findRoomHotelId(final long id) {
        final var result = this.entityManager.createNativeQuery("select hotel_id from room_type where id = :id")
                        .setParameter("id", id)
                        .getResultList();
        return result.isEmpty() ? null : ((Number) result.get(0)).longValue();
    }

    public Long findPlanRoomId(final long id) {
        final var result = this.entityManager.createNativeQuery("select room_type_id from rate_plan where id = :id")
                        .setParameter("id", id)
                        .getResultList();
        return result.isEmpty() ? null : ((Number) result.get(0)).longValue();
    }

    public void upsertRoom(final long id, final long hotelId, final String name, final int maxAdults,
                    final int maxChildren) {
        this.entityManager.createNativeQuery(
                        "insert into room_type (id, hotel_id, name, max_adults, max_children) "
                                        + "values (:id, :hotelId, :name, :maxAdults, :maxChildren) "
                                        + "on conflict (id) do update set hotel_id = excluded.hotel_id, "
                                        + "name = excluded.name, max_adults = excluded.max_adults, "
                                        + "max_children = excluded.max_children")
                        .setParameter("id", id)
                        .setParameter("hotelId", hotelId)
                        .setParameter("name", name)
                        .setParameter("maxAdults", maxAdults)
                        .setParameter("maxChildren", maxChildren)
                        .executeUpdate();
        syncRoomTypeSequence();
    }

    public void upsertPlan(final long id, final long roomTypeId, final String name, final String board,
                    final String cancellationPolicy) {
        this.entityManager.createNativeQuery(
                        "insert into rate_plan (id, room_type_id, name, board, cancellation_policy) "
                                        + "values (:id, :roomTypeId, :name, :board, :policy) "
                                        + "on conflict (id) do update set room_type_id = excluded.room_type_id, "
                                        + "name = excluded.name, board = excluded.board, "
                                        + "cancellation_policy = excluded.cancellation_policy")
                        .setParameter("id", id)
                        .setParameter("roomTypeId", roomTypeId)
                        .setParameter("name", name)
                        .setParameter("board", board)
                        .setParameter("policy", cancellationPolicy)
                        .executeUpdate();
        syncRatePlanSequence();
    }

    /**
     * Reposiciona a sequence depois de um insert com id explicito.
     *
     * <p>Um statement literal por tabela, de proposito: concatenar o nome da tabela no SQL
     * vira superficie de injecao (e o analisador esta certo em reclamar).</p>
     */
    private void syncHotelSequence() {
        this.entityManager.createNativeQuery(
                        "select setval(pg_get_serial_sequence('hotel', 'id'), "
                                        + "greatest((select coalesce(max(id), 1) from hotel), 1))")
                        .getSingleResult();
    }

    private void syncRoomTypeSequence() {
        this.entityManager.createNativeQuery(
                        "select setval(pg_get_serial_sequence('room_type', 'id'), "
                                        + "greatest((select coalesce(max(id), 1) from room_type), 1))")
                        .getSingleResult();
    }

    private void syncRatePlanSequence() {
        this.entityManager.createNativeQuery(
                        "select setval(pg_get_serial_sequence('rate_plan', 'id'), "
                                        + "greatest((select coalesce(max(id), 1) from rate_plan), 1))")
                        .getSingleResult();
    }

    /** Marca de tempo usada para compor o detalhe devolvido pelo admin. */
    public Instant now() {
        return Instant.now();
    }
}
