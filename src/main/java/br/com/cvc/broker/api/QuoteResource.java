package br.com.cvc.broker.api;

import br.com.cvc.broker.api.dto.QuoteRequest;
import br.com.cvc.broker.api.dto.QuoteResponse;
import br.com.cvc.broker.quoting.QuoteService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Cotacao de estadia. */
@Path("/v1/quotes")
public class QuoteResource {

    private final QuoteService quoteService;

    public QuoteResource(final QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public QuoteResponse quote(@Valid final QuoteRequest request) {
        return this.quoteService.quote(request);
    }
}
