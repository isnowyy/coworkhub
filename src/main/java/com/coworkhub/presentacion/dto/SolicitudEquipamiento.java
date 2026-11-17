package com.coworkhub.presentacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar un equipamiento. */
public record SolicitudEquipamiento(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre) {
}
