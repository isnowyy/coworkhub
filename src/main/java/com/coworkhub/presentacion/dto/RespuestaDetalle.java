package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;

import com.coworkhub.negocio.model.BookingItem;

/** Un servicio dentro de una reserva, con el precio que tenía al agregarlo. */
public record RespuestaDetalle(Long servicioId, String servicio, int cantidad, BigDecimal precioUnitarioAplicado,
                               BigDecimal subtotal) {

    public static RespuestaDetalle desde(BookingItem item) {
        return new RespuestaDetalle(item.getService().getId(), item.getService().getName(), item.getQuantity(),
                item.getAppliedUnitPrice(), item.getSubtotal());
    }
}
