package br.com.cvc.broker.api;

import java.util.List;

import br.com.cvc.broker.api.dto.HotelPage;
import br.com.cvc.broker.exceptions.ProblemDetail;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Catalogo de hoteis. Em MB0 a leitura e vazia de proposito: nao existe banco ainda.
 * MB1 troca este vazio pelo read model.
 */
@Path("/v1/hotels")
@Produces(MediaType.APPLICATION_JSON)
public class HotelResource {

    @GET
    public HotelPage list(@QueryParam("cityCode") final String cityCode,
                    @QueryParam("limit") @DefaultValue("20") final int limit,
                    @QueryParam("offset") @DefaultValue("0") final int offset) {
        return new HotelPage(List.of(), new HotelPage.Page(limit, offset, 0));
    }

    @GET
    @Path("/{hotelId}")
    public Response get(@PathParam("hotelId") final long hotelId) {
        return ProblemDetail.typed("hotel-not-found", 404, "No hotel with id " + hotelId, null).toResponse();
    }
}
