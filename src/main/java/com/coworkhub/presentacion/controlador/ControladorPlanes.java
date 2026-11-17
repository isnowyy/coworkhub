package com.coworkhub.presentacion.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.coworkhub.negocio.service.ServicioPlanes;
import com.coworkhub.presentacion.dto.SolicitudPlan;
import com.coworkhub.presentacion.dto.RespuestaPlan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/planes")
@Tag(name = "Planes", description = "Planes de membresía: horas incluidas, descuento en excedentes y máximo de reservas activas. Lectura: cualquier usuario autenticado. Escritura: solo ADMIN.")
public class ControladorPlanes {

    private final ServicioPlanes servicio;

    public ControladorPlanes(ServicioPlanes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar planes", description = "RF-01. Cualquier usuario autenticado.")
    public List<RespuestaPlan> listar() {
        return servicio.listar().stream().map(RespuestaPlan::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un plan", description = "RF-01. Cualquier usuario autenticado.")
    public RespuestaPlan buscar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        return RespuestaPlan.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un plan", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaPlan crear(@Valid @RequestBody SolicitudPlan solicitud) {
        return RespuestaPlan.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un plan", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaPlan actualizar(@Parameter(description = "Id", example = "1") @PathVariable Long id,
                         @Valid @RequestBody SolicitudPlan solicitud) {
        return RespuestaPlan.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un plan", description = "RF-01. Solo ADMIN. No se elimina si está en uso (RN-12). Responde 200 sin cuerpo.")
    public void eliminar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        servicio.eliminar(id);
    }
}
