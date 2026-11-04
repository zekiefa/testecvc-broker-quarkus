package br.com.cvc.broker.api;

import br.com.cvc.broker.exceptions.ProblemDetail;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rotas publicadas no contrato que ainda nao tem implementacao.
 *
 * <p>Existem para nao mentir: o contrato promete o caminho, e o servico responde
 * {@code 501 not-implemented} com {@code type} estavel em vez de 404 ambiguo.</p>
 */
public final class PendingResources {

    private PendingResources() {
    }

    private static Response pending(final String what) {
        return ProblemDetail.typed("not-implemented", 501, what + " is not implemented yet", null).toResponse();
    }

    @Path("/v1")
    @Produces(MediaType.APPLICATION_JSON)
    public static class Availability {

        @GET
        @Path("/availability")
        public Response availability() {
            return pending("GET /v1/availability");
        }
    }

    @Path("/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public static class Admin {

        @PUT
        @Path("/hotels/{hotelId}")
        public Response upsertHotel() {
            return pending("PUT /admin/hotels/{hotelId}");
        }

        @PUT
        @Path("/rooms/{roomTypeId}/rates")
        public Response rates() {
            return pending("PUT /admin/rooms/{roomTypeId}/rates");
        }

        @PUT
        @Path("/rooms/{roomTypeId}/availability")
        public Response availability() {
            return pending("PUT /admin/rooms/{roomTypeId}/availability");
        }
    }
}
