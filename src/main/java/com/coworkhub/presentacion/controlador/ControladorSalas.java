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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.coworkhub.negocio.service.ServicioSalas;
import com.coworkhub.presentacion.dto.SolicitudSala;
import com.coworkhub.presentacion.dto.RespuestaSala;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salas")
@Tag(name = "Salas", description = "Salas de cada sede con su equipamiento. El nombre es único dentro de la sede. Lectura: cualquier usuario autenticado. Escritura: solo ADMIN.")
public class ControladorSalas {

    private final ServicioSalas servicio;

    public ControladorSalas(ServicioSalas servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar salas", description = "RF-01. Cualquier usuario autenticado. Con sedeId, solo las de esa sede.")
    public List<RespuestaSala> listar(
            @Parameter(description = "Filtra por sede (opcional)", example = "1") @RequestParam(required = false) Long sedeId) {
        return servicio.listar(sedeId).stream().map(RespuestaSala::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una sala", description = "RF-01. Cualquier usuario autenticado.")
    public RespuestaSala buscar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        return RespuestaSala.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una sala", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaSala crear(@Valid @RequestBody SolicitudSala solicitud) {
        return RespuestaSala.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una sala", description = "RF-01. Solo ADMIN. Nombre único (RN-11).")
    public RespuestaSala actualizar(@Parameter(description = "Id", example = "1") @PathVariable Long id,
                         @Valid @RequestBody SolicitudSala solicitud) {
        return RespuestaSala.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una sala", description = "RF-01. Solo ADMIN. No se elimina si está en uso (RN-12). Responde 200 sin cuerpo.")
    public void eliminar(@Parameter(description = "Id", example = "1") @PathVariable Long id) {
        servicio.eliminar(id);
    }
}
