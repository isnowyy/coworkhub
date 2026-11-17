package com.coworkhub.presentacion.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar una sede. */
public record SolicitudSede(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) @Schema(example = "Sede Cabecera") String nombre,
        @NotBlank(message = "La ciudad es obligatoria") @Size(max = 80) @Schema(example = "Bucaramanga") String ciudad,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 150) @Schema(example = "Carrera 33 # 48-20") String direccion,
        @NotNull(message = "La hora de apertura es obligatoria") @Schema(example = "07:00:00") LocalTime horaApertura,
        @NotNull(message = "La hora de cierre es obligatoria") @Schema(example = "20:00:00") LocalTime horaCierre) {
}
