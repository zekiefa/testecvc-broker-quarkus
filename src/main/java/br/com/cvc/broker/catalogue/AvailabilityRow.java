package br.com.cvc.broker.catalogue;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Disponibilidade publicada de um quarto numa noite. */
@Entity
@Table(name = "availability")
@IdClass(AvailabilityRow.Key.class)
public class AvailabilityRow {

    @Id
    @Column(name = "room_type_id")
    public Long roomTypeId;

    @Id
    @Column(name = "stay_date")
    public LocalDate stayDate;

    public int allotment;

    @Column(name = "stop_sell")
    public boolean stopSell;

    @Column(name = "updated_at")
    public Instant updatedAt;

    public record Key(Long roomTypeId, LocalDate stayDate) implements java.io.Serializable {
    }
}
