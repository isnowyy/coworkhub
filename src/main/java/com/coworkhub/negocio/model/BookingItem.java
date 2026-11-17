package com.coworkhub.negocio.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Detalle de una reserva: entidad intermedia entre Booking y AdditionalService con datos
 * propios (cantidad y precio aplicado). Pertenece a la reserva: se guarda y se borra con ella
 * (cascade + orphanRemoval desde Booking). Un mismo servicio aparece una sola vez por reserva;
 * si se vuelve a agregar, se suma la cantidad.
 */
@Entity
@Table(name = "booking_items",
       uniqueConstraints = @UniqueConstraint(name = "uk_item_booking_service", columnNames = {"booking_id", "service_id"}))
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    /** Precio del servicio en el momento de agregarlo (RN-07). */
    @Column(name = "applied_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal appliedUnitPrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private AdditionalService service;

    protected BookingItem() {
        // requerido por JPA
    }

    public BookingItem(AdditionalService service, int quantity) {
        this.service = service;
        this.quantity = quantity;
        this.appliedUnitPrice = service.getUnitPrice();
    }

    public Long getId() { return id; }

    public int getQuantity() { return quantity; }

    public void addQuantity(int extra) { this.quantity += extra; }

    public BigDecimal getAppliedUnitPrice() { return appliedUnitPrice; }

    public BigDecimal getSubtotal() {
        return appliedUnitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Booking getBooking() { return booking; }
    void setBooking(Booking booking) { this.booking = booking; }

    public AdditionalService getService() { return service; }
}
