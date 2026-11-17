package com.coworkhub.presentacion.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Datos para crear una reserva, con sus servicios adicionales (opcionales). */
public record SolicitudReserva(
        @NotNull(message = "El miembro es obligatorio") Long miembroId,
        @NotNull(message = "La sala es obligatoria") Long salaId,
        @NotNull(message = "El inicio es obligatorio") LocalDateTime inicio,
        @NotNull(message = "El fin es obligatorio") LocalDateTime fin,
        @NotNull(message = "Los asistentes son obligatorios")
        @Min(value = 1, message = "Debe haber al menos 1 asistente") Integer asistentes,
        List<@Valid @NotNull ItemServicio> servicios) {
}
