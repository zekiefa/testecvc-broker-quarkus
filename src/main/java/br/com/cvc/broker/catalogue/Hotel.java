package br.com.cvc.broker.catalogue;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

/** Hotel publicado. {@code dataSource} diz se veio de seed ou de push. */
@Entity
@Table(name = "hotel")
public class Hotel extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    @Column(name = "city_code")
    public String cityCode;

    @Column(name = "city_name")
    public String cityName;

    public BigDecimal latitude;
    public BigDecimal longitude;

    @Column(name = "data_source")
    public String dataSource;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
