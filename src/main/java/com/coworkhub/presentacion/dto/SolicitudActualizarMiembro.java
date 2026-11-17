package com.coworkhub.presentacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que se pueden cambiar de un miembro: nombre, email y plan. */
public record SolicitudActualizarMiembro(
        @NotBlank(message = "El nombre completo es obligatorio") @Size(max = 120) String nombreCompleto,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") @Size(max = 120) String email,
        @NotNull(message = "El plan es obligatorio") Long planId) {
}
