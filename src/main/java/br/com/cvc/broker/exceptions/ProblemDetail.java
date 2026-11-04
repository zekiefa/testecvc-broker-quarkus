package br.com.cvc.broker.exceptions;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.MDC;

/**
 * RFC 7807 — mesmo formato do servico de reservas, com {@code traceId} como extensao.
 *
 * <p>{@code type} e estavel por classe de problema: o cliente programa contra o tipo,
 * nao contra a mensagem.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(URI type, String title, int status, String detail, URI instance, String traceId) {

    public static final String MEDIA_TYPE = "application/problem+json";
    public static final String PROBLEM_TYPE_BASE = "https://broker.testecvc.example/problems/";

    public static ProblemDetail of(final int status, final String detail, final URI instance) {
        return new ProblemDetail(URI.create("about:blank"), reasonOf(status), status, detail, instance, currentTraceId());
    }

    public static ProblemDetail typed(final String slug, final int status, final String detail, final URI instance) {
        return new ProblemDetail(URI.create(PROBLEM_TYPE_BASE + slug), reasonOf(status), status, detail, instance,
                        currentTraceId());
    }

    public Response toResponse() {
        return Response.status(this.status)
                        .type(MEDIA_TYPE)
                        .entity(this)
                        .build();
    }

    public static String currentTraceId() {
        return MDC.get("traceId");
    }

    private static String reasonOf(final int status) {
        final var reason = Response.Status.fromStatusCode(status);
        return reason == null ? "Error" : reason.getReasonPhrase();
    }

    public static MediaType mediaType() {
        return MediaType.valueOf(MEDIA_TYPE);
    }
}
