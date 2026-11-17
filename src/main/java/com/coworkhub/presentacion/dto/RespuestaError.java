package com.coworkhub.presentacion.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Cuerpo único de todos los errores de la API. codigo es la regla violada (RN-01…) o el tipo
 * de error (VALIDACION, NO_ENCONTRADO…).
 */
public record RespuestaError(String error, String codigo, Instant timestamp) {

    public static RespuestaError de(String error, String codigo) {
        return new RespuestaError(error, codigo, Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }
}
