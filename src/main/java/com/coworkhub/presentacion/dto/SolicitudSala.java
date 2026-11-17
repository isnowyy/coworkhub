package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;
import java.util.List;

import com.coworkhub.negocio.model.RoomType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar una sala con su equipamiento. */
public record SolicitudSala(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) @Schema(example = "Sala Lebrija") String nombre,
        @NotNull(message = "El tipo es obligatorio (MEETING_ROOM, PRIVATE_OFFICE o DESK)") @Schema(example = "MEETING_ROOM") RoomType tipo,
        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1") @Schema(example = "6") Integer capacidad,
        @NotNull(message = "La tarifa por hora es obligatoria")
        @DecimalMin(value = "0.00", inclusive = false, message = "La tarifa por hora debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2) @Schema(example = "30000.00") BigDecimal tarifaHora,
        @Schema(example = "true") Boolean activa,
        @NotNull(message = "La sede es obligatoria") @Schema(example = "1") Long sedeId,
        @Schema(example = "[1, 3]") List<@NotNull Long> equipamientoIds) {
}
