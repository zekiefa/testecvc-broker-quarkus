package br.com.cvc.broker.api;

import java.util.List;

import br.com.cvc.broker.api.dto.HotelDetail;
import br.com.cvc.broker.api.dto.HotelPage;
import br.com.cvc.broker.catalogue.CatalogueRepository;
import br.com.cvc.broker.catalogue.Hotel;
import br.com.cvc.broker.exceptions.BrokerException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Catalogo: lista por cidade e detalhe com quartos e regimes. */
@Path("/v1/hotels")
@Produces(MediaType.APPLICATION_JSON)
public class HotelResource {

    private final CatalogueRepository catalogue;

    public HotelResource(final CatalogueRepository catalogue) {
        this.catalogue = catalogue;
    }

    @GET
    public HotelPage list(@QueryParam("cityCode") final String cityCode,
                    @QueryParam("limit") @DefaultValue("20") final int limit,
                    @QueryParam("offset") @DefaultValue("0") final int offset) {
        final var hotels = this.catalogue.hotels(cityCode, limit, offset);

        return new HotelPage(hotels.stream().map(HotelResource::summary).toList(),
                        new HotelPage.Page(limit, offset, this.catalogue.countHotels(cityCode)));
    }

    @GET
    @Path("/{hotelId}")
    public HotelDetail get(@PathParam("hotelId") final long hotelId) {
        final var hotel = this.catalogue.hotel(hotelId)
                        .orElseThrow(() -> new BrokerException("hotel-not-found", 404,
                                        "No hotel with id " + hotelId));

        final List<HotelDetail.Room> rooms = this.catalogue.roomsOf(hotel.id).stream()
                        .map(room -> new HotelDetail.Room(room.id, room.name, room.maxAdults, room.maxChildren,
                                        this.catalogue.plansOf(room.id).stream()
                                                        .map(plan -> new HotelDetail.Plan(plan.id, plan.name,
                                                                        plan.board, plan.cancellationPolicy))
                                                        .toList()))
                        .toList();

        return new HotelDetail(hotel.id, hotel.name, hotel.cityCode, hotel.cityName, hotel.latitude,
                        hotel.longitude, hotel.dataSource, hotel.updatedAt, rooms);
    }

    private static HotelPage.HotelSummary summary(final Hotel hotel) {
        return new HotelPage.HotelSummary(hotel.id, hotel.name, hotel.cityCode, hotel.cityName,
                        hotel.dataSource, hotel.updatedAt);
    }
}
