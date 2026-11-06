package br.com.cvc.broker.web;

import java.util.Locale;

import br.com.cvc.broker.identity.ApiKeyRegistry;
import br.com.cvc.broker.exceptions.ProblemDetail;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Autentica o <em>buyer</em> por API key e autoriza pelo prefixo do caminho:
 * {@code /admin/**} exige escopo {@code admin}; o resto exige {@code read}.
 *
 * <p>A chave nunca aparece na resposta nem no log — so o veredito. Ausente ou invalida
 * devolve o mesmo {@code 401}, sem revelar se a chave existe.</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    private static final String SCOPE_ADMIN = "admin";
    private static final String SCOPE_READ = "read";

    private final ApiKeyRegistry registry;

    public ApiKeyFilter(final ApiKeyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        final var path = normalize(request.getUriInfo().getPath());
        if (path.startsWith("q/")) {
            return; // endpoints operacionais nao pedem chave
        }

        final var provided = request.getHeaderString(HEADER);
        if (provided == null || provided.isBlank()) {
            request.abortWith(unauthorized());
            return;
        }

        final var scopes = this.registry.findByKey(provided);
        if (scopes.isEmpty()) {
            request.abortWith(unauthorized());
            return;
        }

        final var requiredScope = requiresAdmin(path) ? SCOPE_ADMIN : SCOPE_READ;
        if (!scopes.get().contains(requiredScope)) {
            request.abortWith(forbidden(requiredScope));
        }
    }

    /** JAX-RS entrega o caminho com barra inicial; comparar sempre sem ela. */
    private static String normalize(final String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static boolean requiresAdmin(final String path) {
        return path.toLowerCase(Locale.ROOT).startsWith("admin/");
    }

    private static Response unauthorized() {
        return ProblemDetail.typed("unauthorized", 401, "Missing or invalid API key", null).toResponse();
    }

    private static Response forbidden(final String requiredScope) {
        return ProblemDetail.typed("insufficient-scope", 403, "API key lacks scope: " + requiredScope, null)
                        .toResponse();
    }

}
