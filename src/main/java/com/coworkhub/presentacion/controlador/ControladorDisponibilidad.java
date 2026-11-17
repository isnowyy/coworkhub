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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/disponibilidad")
@Tag(name = "Disponibilidad", description = "Salas libres para reservar.")
public class ControladorDisponibilidad {

    private final ServicioDisponibilidad servicio;

    public ControladorDisponibilidad(ServicioDisponibilidad servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Salas libres de una sede", description = "RF-03. Cualquier usuario autenticado. Salas activas, con capacidad suficiente, con todo el equipamiento pedido y sin reservas activas que se crucen con el rango.")
    public List<RespuestaSala> salasLibres(
            @Parameter(description = "Sede", example = "1") @RequestParam Long sedeId,
            @Parameter(description = "Fecha yyyy-MM-dd", example = "2026-12-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Hora de inicio HH:mm", example = "10:00") @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @Parameter(description = "Hora de fin HH:mm", example = "11:00") @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            @Parameter(description = "Capacidad mínima (opcional)", example = "4") @RequestParam(required = false) Integer capacidadMinima,
            @Parameter(description = "Ids de equipamiento que la sala debe tener (opcional)", example = "4") @RequestParam(required = false) List<Long> equipamientoIds) {
        return servicio.salasLibres(sedeId, fecha, horaInicio, horaFin, capacidadMinima, equipamientoIds)
                .stream().map(RespuestaSala::desde).toList();
    }
}
