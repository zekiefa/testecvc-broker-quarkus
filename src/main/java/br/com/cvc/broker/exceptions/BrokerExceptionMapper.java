package br.com.cvc.broker.exceptions;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Traduz erro de dominio em problem+json com {@code type} estavel. */
@Provider
public class BrokerExceptionMapper implements ExceptionMapper<BrokerException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(final BrokerException exception) {
        final URI instance = this.uriInfo == null ? null : this.uriInfo.getRequestUri();

        if (exception instanceof IncompleteRatePeriodException incomplete) {
            final List<String> dates = incomplete.missingDates().stream().map(Object::toString).toList();
            return ProblemDetail.typedWithMissingDates(exception.slug(), exception.status(),
                            exception.getMessage(), instance, dates)
                            .toResponse();
        }

        return ProblemDetail.typed(exception.slug(), exception.status(), exception.getMessage(), instance)
                        .toResponse();
    }
}
