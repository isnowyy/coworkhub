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

import com.coworkhub.negocio.service.ServicioEquipamientos;
import com.coworkhub.presentacion.dto.SolicitudEquipamiento;
import com.coworkhub.presentacion.dto.RespuestaEquipamiento;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/equipamientos")
@Tag(name = "Equipamientos", description = "Equipamiento que se asigna a las salas. Lectura: cualquier usuario autenticado. Escritura: solo ADMIN.")
public class ControladorEquipamientos {

    private final ServicioEquipamientos servicio;

    public ControladorEquipamientos(ServicioEquipamientos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar equipamientos", description = "RF-01. Cualquier usuario autenticado.")
    public List<RespuestaEquipamiento> listar() {
        return servicio.listar().stream().map(RespuestaEquipamiento::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un equipamiento", description = "RF-01. Cualquier usuario autenticado.")
    public RespuestaEquipamiento buscar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        return RespuestaEquipamiento.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un equipamiento", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaEquipamiento crear(@Valid @RequestBody SolicitudEquipamiento solicitud) {
        return RespuestaEquipamiento.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un equipamiento", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaEquipamiento actualizar(@Parameter(description = "Id", example = "1") @PathVariable Long id,
                         @Valid @RequestBody SolicitudEquipamiento solicitud) {
        return RespuestaEquipamiento.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un equipamiento", description = "RF-01. Solo ADMIN. No se elimina si está en uso (RN-12). Responde 200 sin cuerpo.")
    public void eliminar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        servicio.eliminar(id);
    }
}
