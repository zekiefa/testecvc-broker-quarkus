package br.com.cvc.broker.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Erros do proprio JAX-RS (rota inexistente, 405, 415) saem como RFC 7807 com about:blank. */
@Provider
public class EvaluationExceptionHandler implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(final WebApplicationException exception) {
        final var status = exception.getResponse() == null
                        ? Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
                        : exception.getResponse().getStatus();
        final var detail = exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "Request could not be processed"
                        : exception.getMessage();

        return ProblemDetail.of(status, detail, this.uriInfo == null ? null : this.uriInfo.getRequestUri())
                        .toResponse();
    }
}
