package com.coworkhub.presentacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Un servicio adicional con su cantidad, dentro de una reserva. */
public record ItemServicio(
        @NotNull(message = "El servicio es obligatorio") Long servicioId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 500, message = "La cantidad no puede superar 500") Integer cantidad) {
}
