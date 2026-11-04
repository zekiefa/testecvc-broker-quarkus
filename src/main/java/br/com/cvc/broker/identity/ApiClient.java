package br.com.cvc.broker.identity;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

/** Comprador autorizado. Guarda apenas o hash da chave. */
@Entity
@Table(name = "api_client")
public class ApiClient extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    @Column(name = "key_hash")
    public String keyHash;

    public String scopes;

    public boolean enabled;

    @Column(name = "rate_limit_per_min")
    public int rateLimitPerMin;

    @Column(name = "created_at")
    public Instant createdAt;

    public Set<String> scopeSet() {
        return Arrays.stream(this.scopes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
    }
}
