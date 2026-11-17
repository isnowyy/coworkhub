package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;

import com.coworkhub.negocio.model.AdditionalService;

public record RespuestaServicioAdicional(Long id, String nombre, BigDecimal precioUnitario) {

    public static RespuestaServicioAdicional desde(AdditionalService servicio) {
        return new RespuestaServicioAdicional(servicio.getId(), servicio.getName(), servicio.getUnitPrice());
    }
}
