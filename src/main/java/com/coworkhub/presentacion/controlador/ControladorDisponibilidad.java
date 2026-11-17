package com.coworkhub.presentacion.controlador;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coworkhub.negocio.service.ServicioDisponibilidad;
import com.coworkhub.presentacion.dto.RespuestaSala;

@RestController
@RequestMapping("/api/disponibilidad")
public class ControladorDisponibilidad {

    private final ServicioDisponibilidad servicio;

    public ControladorDisponibilidad(ServicioDisponibilidad servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaSala> salasLibres(
            @RequestParam Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            @RequestParam(required = false) Integer capacidadMinima,
            @RequestParam(required = false) List<Long> equipamientoIds) {
        return servicio.salasLibres(sedeId, fecha, horaInicio, horaFin, capacidadMinima, equipamientoIds)
                .stream().map(RespuestaSala::desde).toList();
    }
}
