package com.coworkhub.presentacion.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.coworkhub.negocio.model.MemberStatus;
import com.coworkhub.negocio.service.ServicioConsumo;
import com.coworkhub.negocio.service.ServicioMiembros;
import com.coworkhub.presentacion.dto.RespuestaConsumo;
import com.coworkhub.presentacion.dto.RespuestaMiembro;
import com.coworkhub.presentacion.dto.SolicitudActualizarMiembro;
import com.coworkhub.presentacion.dto.SolicitudMiembro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/miembros")
@Tag(name = "Miembros", description = "Clientes del coworking con su plan y su cuenta. Los gestionan ADMIN y RECEPTIONIST; un MEMBER solo ve su ficha y su consumo.")
public class ControladorMiembros {

    private final ServicioMiembros servicio;
    private final ServicioConsumo servicioConsumo;

    public ControladorMiembros(ServicioMiembros servicio, ServicioConsumo servicioConsumo) {
        this.servicio = servicio;
        this.servicioConsumo = servicioConsumo;
    }

    @GetMapping
    @Operation(summary = "Listar miembros", description = "RF-02. ADMIN y RECEPTIONIST. Filtros opcionales por nombre (contiene) y estado.")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public List<RespuestaMiembro> listar(
            @Parameter(description = "Parte del nombre", example = "gómez") @RequestParam(required = false) String nombre,
            @Parameter(description = "Estado", example = "SUSPENDED") @RequestParam(required = false) MemberStatus estado) {
        return servicio.listar(nombre, estado).stream().map(RespuestaMiembro::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un miembro", description = "RF-02. ADMIN y RECEPTIONIST; un MEMBER solo el suyo.")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @permisos.esSuMiembro(#id)")
    public RespuestaMiembro buscar(@PathVariable Long id) {
        return RespuestaMiembro.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un miembro y su cuenta", description = "RF-02. ADMIN y RECEPTIONIST. Crea el miembro (ACTIVE) y su cuenta con rol MEMBER. Documento, email y usuario únicos (RN-11).")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public RespuestaMiembro crear(@Valid @RequestBody SolicitudMiembro solicitud) {
        return RespuestaMiembro.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre, email y plan", description = "RF-02. ADMIN y RECEPTIONIST.")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public RespuestaMiembro actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudActualizarMiembro solicitud) {
        return RespuestaMiembro.desde(servicio.actualizar(id, solicitud));
    }

    @PatchMapping("/{id}/suspender")
    @Operation(summary = "Suspender un miembro", description = "RF-02. ADMIN y RECEPTIONIST. Un miembro suspendido no puede reservar (RN-05).")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public RespuestaMiembro suspender(@PathVariable Long id) {
        return RespuestaMiembro.desde(servicio.suspender(id));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Reactivar un miembro", description = "RF-02. ADMIN y RECEPTIONIST.")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public RespuestaMiembro activar(@PathVariable Long id) {
        return RespuestaMiembro.desde(servicio.activar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un miembro", description = "RF-02. ADMIN y RECEPTIONIST. No se elimina si tiene reservas (RN-12). Responde 200 sin cuerpo.")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    @GetMapping("/{id}/consumo")
    @Operation(summary = "Consumo mensual de horas", description = "RF-10. ADMIN y RECEPTIONIST; un MEMBER solo el suyo. Horas incluidas, usadas, restantes y excedentes.")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @permisos.esSuMiembro(#id)")
    public RespuestaConsumo consumo(@PathVariable Long id,
                                    @Parameter(description = "Mes yyyy-MM (por defecto, el actual)", example = "2026-11") @RequestParam(required = false) String mes) {
        return RespuestaConsumo.desde(servicioConsumo.consultar(id, mes));
    }
}
