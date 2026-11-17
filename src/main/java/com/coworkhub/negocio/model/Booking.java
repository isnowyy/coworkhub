package com.coworkhub.negocio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Reserva.
 * <ul>
 * <li>Booking → Room y Booking → Member: Booking es el dueño (FK room_id y member_id). LAZY,
 * sin cascade: la reserva no crea ni borra salas ni miembros.</li>
 * <li>Booking → BookingItem: composición. El dueño de la FK es BookingItem (mappedBy), pero la
 * reserva controla el ciclo de vida: cascade ALL guarda los detalles con la reserva y
 * orphanRemoval borra de la base el detalle que se quita de la lista (RF-06).</li>
 * </ul>
 * consumedMinutes no está en el enunciado: guarda cuántos minutos del plan consumió la
 * reserva, para poder devolverlos al cancelar con 24 h o más (RN-08) y calcular el consumo
 * mensual (RF-10) con una sola suma.
 */
@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "ix_booking_room_start", columnList = "room_id, start_time"),
           @Index(name = "ix_booking_member_start", columnList = "member_id, start_time")
       })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private int attendees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "room_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal roomCost = BigDecimal.ZERO;

    @Column(name = "services_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicesCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "cancellation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    @Column(name = "consumed_minutes", nullable = false)
    private int consumedMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingItem> items = new ArrayList<>();

    protected Booking() {
        // requerido por JPA
    }

    public Booking(Room room, Member member, LocalDateTime startTime, LocalDateTime endTime, int attendees) {
        this.room = room;
        this.member = member;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendees = attendees;
    }

    /** Agrega un detalle y mantiene los dos lados de la relación sincronizados. */
    public void addItem(BookingItem item) {
        items.add(item);
        item.setBooking(this);
    }

    /** Quita un detalle; orphanRemoval lo elimina de la base al confirmar la transacción. */
    public void removeItem(BookingItem item) {
        items.remove(item);
        item.setBooking(null);
    }

    public Long getId() { return id; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public int getAttendees() { return attendees; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public BigDecimal getRoomCost() { return roomCost; }
    public void setRoomCost(BigDecimal roomCost) { this.roomCost = roomCost; }

    public BigDecimal getServicesCost() { return servicesCost; }
    public void setServicesCost(BigDecimal servicesCost) { this.servicesCost = servicesCost; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }

    public int getConsumedMinutes() { return consumedMinutes; }
    public void setConsumedMinutes(int consumedMinutes) { this.consumedMinutes = consumedMinutes; }

    public Room getRoom() { return room; }

    public Member getMember() { return member; }

    public List<BookingItem> getItems() { return Collections.unmodifiableList(items); }
}
