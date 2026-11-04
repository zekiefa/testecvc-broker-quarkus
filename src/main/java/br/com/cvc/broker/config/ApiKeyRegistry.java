package br.com.cvc.broker.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Registro de chaves do MB0: vem da configuracao, nao do banco.
 *
 * <p>Duas chaves de desenvolvimento — uma com escopo {@code read}, outra com {@code admin}.
 * MB2 troca esta classe por leitura de {@code api_client} com hash; o resto do codigo nao muda,
 * porque so conhece {@link #findByKey(String)}.</p>
 */
@ApplicationScoped
public class ApiKeyRegistry {

    private final String readKey;
    private final String adminKey;

    public ApiKeyRegistry(
                    @ConfigProperty(name = "broker.api-keys.read.key") final String readKey,
                    @ConfigProperty(name = "broker.api-keys.admin.key") final String adminKey) {
        this.readKey = readKey;
        this.adminKey = adminKey;
    }

    /** Compara em tempo constante: o custo nao deve revelar quantos caracteres acertaram. */
    public Optional<Set<String>> findByKey(final String provided) {
        if (provided == null || provided.isBlank()) {
            return Optional.empty();
        }
        if (matches(this.adminKey, provided)) {
            return Optional.of(Set.of("read", "admin"));
        }
        if (matches(this.readKey, provided)) {
            return Optional.of(Set.of("read"));
        }
        return Optional.empty();
    }

    private static boolean matches(final String expected, final String provided) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                        provided.getBytes(StandardCharsets.UTF_8));
    }
}
