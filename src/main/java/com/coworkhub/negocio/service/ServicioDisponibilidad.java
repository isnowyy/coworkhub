package com.coworkhub.negocio.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.coworkhub.negocio.model.Room;

/** Salas libres de una sede en una fecha y rango horario (RF-03). */
public interface ServicioDisponibilidad {

    List<Room> salasLibres(Long sedeId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                           Integer capacidadMinima, List<Long> equipamientoIds);
}
