package br.com.cvc.broker.catalogue;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.panache.common.Sort;

/** Leitura do catalogo. Sem regra de negocio: quem decide e o caso de uso. */
@ApplicationScoped
public class CatalogueRepository {

    /** Teto de página: acima disso a resposta vira payload sem uso. */
    private static final int MAX_PAGE_SIZE = 100;

    public List<Hotel> hotels(final String cityCode, final int limit, final int offset) {
        // range() e offset/limite; page() seria indice de pagina. Ordenacao fixa para a
        // paginacao ser estavel entre chamadas.
        final var sort = Sort.by("id");
        final var query = cityCode == null || cityCode.isBlank()
                        ? Hotel.findAll(sort)
                        : Hotel.find("cityCode", sort, cityCode);

        final int safeLimit = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        return query.range(offset, offset + safeLimit - 1).list();
    }

    public long countHotels(final String cityCode) {
        return cityCode == null || cityCode.isBlank()
                        ? Hotel.count()
                        : Hotel.count("cityCode", cityCode);
    }

    public Optional<Hotel> hotel(final long hotelId) {
        return Hotel.findByIdOptional(hotelId);
    }

    public List<RoomType> roomsOf(final long hotelId) {
        return RoomType.list("hotelId", hotelId);
    }

    public Optional<RoomType> room(final long roomTypeId) {
        return RoomType.findByIdOptional(roomTypeId);
    }

    public List<RatePlan> plansOf(final long roomTypeId) {
        return RatePlan.list("roomTypeId", roomTypeId);
    }

    public Optional<RatePlan> plan(final long ratePlanId) {
        return RatePlan.findByIdOptional(ratePlanId);
    }
}
