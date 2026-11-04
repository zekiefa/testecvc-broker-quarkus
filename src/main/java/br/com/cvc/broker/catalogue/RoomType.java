package br.com.cvc.broker.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "room_type")
public class RoomType extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "hotel_id")
    public Long hotelId;

    public String name;

    @Column(name = "max_adults")
    public int maxAdults;

    @Column(name = "max_children")
    public int maxChildren;
}
