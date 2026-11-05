package br.com.cvc.broker.search;

import java.time.LocalDate;

import br.com.cvc.broker.api.dto.SearchResponse;
import br.com.cvc.broker.exceptions.BrokerException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Busca por destino com preço do período — evita o consumidor fazer N+1. */
@Path("/v1/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    private final SearchService service;

    public SearchResource(final SearchService service) {
        this.service = service;
    }

    @GET
    public SearchResponse search(@QueryParam("cityCode") final String cityCode,
                    @QueryParam("checkIn") final LocalDate checkIn,
                    @QueryParam("checkOut") final LocalDate checkOut,
                    @QueryParam("adults") final Integer adults,
                    @QueryParam("children") final Integer children) {
        if (cityCode == null || cityCode.isBlank() || checkIn == null || checkOut == null || adults == null
                        || children == null) {
            throw new BrokerException("validation-failed", 400,
                            "cityCode, checkIn, checkOut, adults and children are required");
        }

        return this.service.search(cityCode, checkIn, checkOut, adults, children);
    }
}
