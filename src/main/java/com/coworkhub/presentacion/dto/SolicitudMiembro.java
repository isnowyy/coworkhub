package com.coworkhub.presentacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registro de un miembro junto con su cuenta (rol MEMBER). */
public record SolicitudMiembro(
        @NotBlank(message = "El documento es obligatorio")
        @Pattern(regexp = "\\d{5,20}", message = "El documento debe tener entre 5 y 20 dígitos") String documento,
        @NotBlank(message = "El nombre completo es obligatorio") @Size(max = 120) String nombreCompleto,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") @Size(max = 120) String email,
        @NotNull(message = "El plan es obligatorio") Long planId,
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "El usuario debe tener de 3 a 50 letras, números, punto, guion o guion bajo") String usuario,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String contrasena) {
}
