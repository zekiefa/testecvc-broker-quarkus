package br.com.cvc.broker.web;

import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

/**
 * Da a cada requisicao um id correlacionavel: aceita {@code X-Request-Id} do cliente quando
 * e um token simples e curto, senao gera um UUID. O id vai para o MDC, para o header de
 * resposta e para o corpo do erro.
 */
@Provider
@Priority(1)
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "traceId";

    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    public void filter(final ContainerRequestContext request) {
        final var provided = request.getHeaderString(HEADER);
        final var id = provided != null && SAFE_VALUE.matcher(provided).matches()
                        ? provided
                        : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, id);
        request.setProperty(MDC_KEY, id);
    }

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        final var id = (String) request.getProperty(MDC_KEY);
        if (id != null) {
            response.getHeaders().add(HEADER, id);
        }
        MDC.remove(MDC_KEY);
    }
}
