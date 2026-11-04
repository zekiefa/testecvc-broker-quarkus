package br.com.cvc.broker.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Resolve a chave para os escopos do cliente.
 *
 * <p>A chave chega em claro e sai da memoria: o banco guarda apenas o SHA-256. Comparar hash
 * evita varredura por prefixo e significa que um dump do banco nao entrega chaves.</p>
 */
@ApplicationScoped
public class ApiKeyRegistry {

    private final ApiClientRepository repository;

    public ApiKeyRegistry(final ApiClientRepository repository) {
        this.repository = repository;
    }

    public Optional<Set<String>> findByKey(final String provided) {
        if (provided == null || provided.isBlank()) {
            return Optional.empty();
        }

        return this.repository.enabledByKeyHash(hash(provided))
                        .map(ApiClient::scopeSet);
    }

    static String hash(final String key) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel neste runtime", e);
        }
    }
}
