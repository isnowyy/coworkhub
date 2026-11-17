package com.coworkhub.presentacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar un plan de membresía. */
public record SolicitudPlan(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @NotNull(message = "Las horas incluidas son obligatorias")
        @Min(value = 0, message = "Las horas incluidas no pueden ser negativas") Integer horasIncluidasMes,
        @NotNull(message = "El descuento es obligatorio")
        @Min(value = 0, message = "El descuento va de 0 a 100")
        @Max(value = 100, message = "El descuento va de 0 a 100") Integer descuentoExcedente,
        @NotNull(message = "El máximo de reservas activas es obligatorio")
        @Min(value = 1, message = "El máximo de reservas activas debe ser al menos 1") Integer maxReservasActivas) {
}
