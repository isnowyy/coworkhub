package com.coworkhub.negocio.service;

import java.time.LocalDate;
import java.util.List;

import com.coworkhub.negocio.model.Booking;
import com.coworkhub.negocio.model.BookingStatus;
import com.coworkhub.presentacion.dto.ItemServicio;
import com.coworkhub.presentacion.dto.SolicitudReserva;

public interface ServicioReservas {

    /** RF-04: crea la reserva aplicando RN-01 a RN-07. */
    Booking crear(SolicitudReserva solicitud);

    Booking buscar(Long id);

    /** RF-05: exactamente uno de los filtros: miembroId, salaId + fecha, o estado. */
    List<Booking> listar(Long miembroId, Long salaId, LocalDate fecha, BookingStatus estado);

    /** RF-06 y RN-10. */
    Booking agregarServicio(Long reservaId, ItemServicio item);

    /** RF-06 y RN-10. */
    Booking quitarServicio(Long reservaId, Long servicioId);

    /** RF-07 y RN-09. */
    Booking confirmar(Long id);

    /** RF-08, RN-08 y RN-09. */
    Booking cancelar(Long id);

    /** RF-09 y RN-09. */
    Booking completar(Long id);

    /** RN-13: la reserva existe y es del miembro indicado. */
    boolean perteneceA(Long reservaId, Long miembroId);
}
