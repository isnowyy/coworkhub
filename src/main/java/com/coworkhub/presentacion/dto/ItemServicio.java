package com.coworkhub.presentacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Un servicio adicional con su cantidad, dentro de una reserva. */
public record ItemServicio(
        @NotNull(message = "El servicio es obligatorio") @Schema(example = "1") Long servicioId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 500, message = "La cantidad no puede superar 500") @Schema(example = "4") Integer cantidad) {
}
