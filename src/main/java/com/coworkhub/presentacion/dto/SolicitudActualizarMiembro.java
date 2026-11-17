package com.coworkhub.presentacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que se pueden cambiar de un miembro: nombre, email y plan. */
public record SolicitudActualizarMiembro(
        @NotBlank(message = "El nombre completo es obligatorio") @Size(max = 120) @Schema(example = "Ana Gómez Rueda") String nombreCompleto,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") @Size(max = 120) @Schema(example = "ana.gomez@ejemplo.com") String email,
        @NotNull(message = "El plan es obligatorio") @Schema(example = "3") Long planId) {
}
