package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.coworkhub.negocio.model.Booking;
import com.coworkhub.negocio.model.BookingStatus;

public record RespuestaReserva(Long id, LocalDateTime inicio, LocalDateTime fin, int asistentes, BookingStatus estado,
                               Long salaId, String sala, String sede, Long miembroId, String miembro,
                               BigDecimal costoSala, BigDecimal costoServicios, BigDecimal costoTotal,
                               BigDecimal cargoCancelacion, int minutosDelPlanConsumidos,
                               List<RespuestaDetalle> servicios) {

    /** Requiere sala, sede, miembro y detalles cargados (el servicio usa @EntityGraph). */
    public static RespuestaReserva desde(Booking r) {
        return new RespuestaReserva(r.getId(), r.getStartTime(), r.getEndTime(), r.getAttendees(), r.getStatus(),
                r.getRoom().getId(), r.getRoom().getName(), r.getRoom().getLocation().getName(),
                r.getMember().getId(), r.getMember().getFullName(),
                r.getRoomCost(), r.getServicesCost(), r.getTotalCost(), r.getCancellationFee(),
                r.getConsumedMinutes(), r.getItems().stream().map(RespuestaDetalle::desde).toList());
    }
}
