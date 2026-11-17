package com.coworkhub.presentacion.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Datos para crear una reserva, con sus servicios adicionales (opcionales). */
public record SolicitudReserva(
        @NotNull(message = "El miembro es obligatorio") @Schema(example = "1") Long miembroId,
        @NotNull(message = "La sala es obligatoria") @Schema(example = "3") Long salaId,
        @NotNull(message = "El inicio es obligatorio") @Schema(example = "2026-12-01T10:00:00") LocalDateTime inicio,
        @NotNull(message = "El fin es obligatorio") @Schema(example = "2026-12-01T11:30:00") LocalDateTime fin,
        @NotNull(message = "Los asistentes son obligatorios")
        @Min(value = 1, message = "Debe haber al menos 1 asistente") @Schema(example = "3") Integer asistentes,
        List<@Valid @NotNull ItemServicio> servicios) {
}
