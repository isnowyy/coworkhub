package com.coworkhub.negocio.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Sala. El nombre es único dentro de su sede (RN-11).
 * <ul>
 * <li>Room → Location: Room es el dueño (la FK location_id vive en rooms). LAZY y sin cascade:
 * borrar una sala no borra la sede.</li>
 * <li>Room ↔ Equipment: N↔N con tabla intermedia room_equipment; Room es el dueño. Es un Set
 * (no List) para poder traerlo con @EntityGraph junto con otras relaciones sin
 * MultipleBagFetchException. Sin cascade: el equipamiento es un catálogo compartido.</li>
 * </ul>
 */
@Entity
@Table(name = "rooms",
       uniqueConstraints = @UniqueConstraint(name = "uk_room_location_name", columnNames = {"location_id", "name"}))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "hourly_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "room_equipment",
               joinColumns = @JoinColumn(name = "room_id"),
               inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();

    protected Room() {
        // requerido por JPA
    }

    public Room(String name, RoomType type, int capacity, BigDecimal hourlyRate, boolean active, Location location) {
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.active = active;
        this.location = location;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public Set<Equipment> getEquipment() { return equipment; }

    /** Reemplaza el equipamiento de la sala por el indicado. */
    public void replaceEquipment(Set<Equipment> nuevos) {
        equipment.clear();
        equipment.addAll(nuevos);
    }
}
