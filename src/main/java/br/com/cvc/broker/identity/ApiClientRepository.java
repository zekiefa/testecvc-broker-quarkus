package br.com.cvc.broker.identity;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

/** Leitura de clientes por hash. Repositorio explicito: o {@code find} estatico nao devolve tipo. */
@ApplicationScoped
public class ApiClientRepository implements PanacheRepositoryBase<ApiClient, Long> {

    public Optional<ApiClient> enabledByKeyHash(final String keyHash) {
        return find("keyHash = ?1 and enabled = true", keyHash).firstResultOptional();
    }
}
