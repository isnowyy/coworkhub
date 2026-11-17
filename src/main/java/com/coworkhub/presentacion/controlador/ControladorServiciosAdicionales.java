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

import com.coworkhub.negocio.service.ServicioServiciosAdicionales;
import com.coworkhub.presentacion.dto.SolicitudServicioAdicional;
import com.coworkhub.presentacion.dto.RespuestaServicioAdicional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios-adicionales")
@Tag(name = "Servicios adicionales", description = "Servicios que se suman a una reserva. Cambiar el precio no altera reservas existentes. Lectura: cualquier usuario autenticado. Escritura: solo ADMIN.")
public class ControladorServiciosAdicionales {

    private final ServicioServiciosAdicionales servicio;

    public ControladorServiciosAdicionales(ServicioServiciosAdicionales servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar servicios adicionales", description = "RF-01. Cualquier usuario autenticado.")
    public List<RespuestaServicioAdicional> listar() {
        return servicio.listar().stream().map(RespuestaServicioAdicional::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un servicio adicional", description = "RF-01. Cualquier usuario autenticado.")
    public RespuestaServicioAdicional buscar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        return RespuestaServicioAdicional.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un servicio adicional", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaServicioAdicional crear(@Valid @RequestBody SolicitudServicioAdicional solicitud) {
        return RespuestaServicioAdicional.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un servicio adicional", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaServicioAdicional actualizar(@Parameter(description = "Id", example = "1") @PathVariable Long id,
                         @Valid @RequestBody SolicitudServicioAdicional solicitud) {
        return RespuestaServicioAdicional.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un servicio adicional", description = "RF-01. Solo ADMIN. No se elimina si está en uso (RN-12). Responde 200 sin cuerpo.")
    public void eliminar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        servicio.eliminar(id);
    }
}
