package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar un servicio adicional. */
public record SolicitudServicioAdicional(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario) {
}
