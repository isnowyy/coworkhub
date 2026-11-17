package com.coworkhub.negocio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Plan de membresía. Nombre único (RN-11). Los miembros apuntan al plan desde Member
 * (lado dueño); el plan no guarda la colección.
 */
@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** Horas que el plan incluye cada mes y no se cobran (RN-07). */
    @Column(name = "included_hours_per_month", nullable = false)
    private int includedHoursPerMonth;

    /** Descuento sobre las horas excedentes, en porcentaje de 0 a 100 (RN-07). */
    @Column(name = "overage_discount", nullable = false)
    private int overageDiscount;

    /** Máximo de reservas activas futuras (RN-06). */
    @Column(name = "max_active_bookings", nullable = false)
    private int maxActiveBookings;

    protected MembershipPlan() {
        // requerido por JPA
    }

    public MembershipPlan(String name, int includedHoursPerMonth, int overageDiscount, int maxActiveBookings) {
        this.name = name;
        this.includedHoursPerMonth = includedHoursPerMonth;
        this.overageDiscount = overageDiscount;
        this.maxActiveBookings = maxActiveBookings;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIncludedHoursPerMonth() { return includedHoursPerMonth; }
    public void setIncludedHoursPerMonth(int includedHoursPerMonth) { this.includedHoursPerMonth = includedHoursPerMonth; }

    public int getOverageDiscount() { return overageDiscount; }
    public void setOverageDiscount(int overageDiscount) { this.overageDiscount = overageDiscount; }

    public int getMaxActiveBookings() { return maxActiveBookings; }
    public void setMaxActiveBookings(int maxActiveBookings) { this.maxActiveBookings = maxActiveBookings; }
}
