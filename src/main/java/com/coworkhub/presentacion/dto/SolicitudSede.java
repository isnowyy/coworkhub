package com.coworkhub.presentacion.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar una sede. */
public record SolicitudSede(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
        @NotBlank(message = "La ciudad es obligatoria") @Size(max = 80) String ciudad,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 150) String direccion,
        @NotNull(message = "La hora de apertura es obligatoria") LocalTime horaApertura,
        @NotNull(message = "La hora de cierre es obligatoria") LocalTime horaCierre) {
}
