package br.com.cvc.broker.catalogue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Linha de tarifa. Existe como entidade apenas para o JPQL da leitura; a escrita
 * (publicacao de tarifas) vira upsert em MB2.
 */
@Entity
@Table(name = "rate")
@IdClass(RateRow.Key.class)
public class RateRow {

    @Id
    @Column(name = "room_type_id")
    public Long roomTypeId;

    @Id
    @Column(name = "rate_plan_id")
    public Long ratePlanId;

    @Id
    @Column(name = "stay_date")
    public LocalDate stayDate;

    @Column(name = "adult_amount")
    public BigDecimal adultAmount;

    @Column(name = "child_amount")
    public BigDecimal childAmount;

    public String currency;

    @Column(name = "updated_at")
    public Instant updatedAt;

    /** Chave composta. */
    public record Key(Long roomTypeId, Long ratePlanId, LocalDate stayDate) implements java.io.Serializable {
    }
}
