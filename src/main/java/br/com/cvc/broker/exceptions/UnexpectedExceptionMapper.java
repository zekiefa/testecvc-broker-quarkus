package br.com.cvc.broker.exceptions;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Rede de seguranca: excecao nao mapeada vira 500 sem vazar stack trace nem mensagem interna. */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(UnexpectedExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(final Throwable exception) {
        log.error("Unhandled exception while serving the request", exception);
        return ProblemDetail.of(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "Unexpected error; the incident was logged",
                        this.uriInfo == null ? null : this.uriInfo.getRequestUri())
                        .toResponse();
    }
}
