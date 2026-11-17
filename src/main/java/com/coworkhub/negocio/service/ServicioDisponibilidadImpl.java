package com.coworkhub.negocio.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.ReglaNegocioException;
import com.coworkhub.negocio.excepcion.SolicitudInvalidaException;
import com.coworkhub.negocio.model.BookingStatus;
import com.coworkhub.negocio.model.Location;
import com.coworkhub.negocio.model.Room;
import com.coworkhub.persistence.repository.RoomRepository;

@Service
public class ServicioDisponibilidadImpl implements ServicioDisponibilidad {

    private final RoomRepository salas;
    private final ServicioSedes servicioSedes;

    public ServicioDisponibilidadImpl(RoomRepository salas, ServicioSedes servicioSedes) {
        this.salas = salas;
        this.servicioSedes = servicioSedes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> salasLibres(Long sedeId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                  Integer capacidadMinima, List<Long> equipamientoIds) {
        if (!horaInicio.isBefore(horaFin)) {
            throw new SolicitudInvalidaException("La hora de inicio debe ser anterior a la hora de fin");
        }
        Location sede = servicioSedes.buscar(sedeId);
        if (horaInicio.isBefore(sede.getOpeningTime()) || horaFin.isAfter(sede.getClosingTime())) {
            throw new ReglaNegocioException("RN-03", "El rango está fuera del horario de la sede ("
                    + sede.getOpeningTime() + " a " + sede.getClosingTime() + ")");
        }
        int capacidad = capacidadMinima == null ? 1 : Math.max(1, capacidadMinima);
        Set<Long> equiposPedidos = equipamientoIds == null ? Set.of() : new HashSet<>(equipamientoIds);

        return salas.findAvailable(sedeId, capacidad, fecha.atTime(horaInicio), fecha.atTime(horaFin),
                        BookingStatus.ACTIVE)
                .stream()
                .filter(sala -> tieneEquipos(sala, equiposPedidos))
                .toList();
    }

    private static boolean tieneEquipos(Room sala, Set<Long> pedidos) {
        Set<Long> propios = new HashSet<>();
        sala.getEquipment().forEach(e -> propios.add(e.getId()));
        return propios.containsAll(pedidos);
    }
}
