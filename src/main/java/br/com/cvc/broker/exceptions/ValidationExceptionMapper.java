package br.com.cvc.broker.exceptions;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Validacao estrutural: 400 nomeando os campos, para o comprador saber o que corrigir. */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        final var fields = exception.getConstraintViolations().stream()
                        .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                        .sorted()
                        .collect(Collectors.joining("; "));

        return ProblemDetail.typed("validation-failed", 400, fields,
                        this.uriInfo == null ? null : this.uriInfo.getRequestUri())
                        .toResponse();
    }
}
