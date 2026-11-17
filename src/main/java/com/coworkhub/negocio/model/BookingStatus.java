package com.coworkhub.negocio.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Estado de una reserva (RN-09): PENDING → CONFIRMED → COMPLETED, o PENDING/CONFIRMED → CANCELLED.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    /** Estados que ocupan la sala (RN-01) y cuentan como reservas activas (RN-06). */
    public static final Set<BookingStatus> ACTIVE = Collections.unmodifiableSet(EnumSet.of(PENDING, CONFIRMED));
}
