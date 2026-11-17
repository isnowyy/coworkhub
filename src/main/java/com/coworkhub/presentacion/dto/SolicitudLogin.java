package com.coworkhub.presentacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SolicitudLogin(
        @NotBlank(message = "El usuario es obligatorio") @Schema(example = "admin") String usuario,
        @NotBlank(message = "La contraseña es obligatoria") @Schema(example = "admin123") String contrasena) {
}
