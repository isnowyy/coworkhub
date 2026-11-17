package com.coworkhub.presentacion.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/miembros")
public class ControladorMiembros {

    private final ServicioMiembros servicio;
    private final ServicioConsumo servicioConsumo;

    public ControladorMiembros(ServicioMiembros servicio, ServicioConsumo servicioConsumo) {
        this.servicio = servicio;
        this.servicioConsumo = servicioConsumo;
    }

    @GetMapping
    public List<RespuestaMiembro> listar(@RequestParam(required = false) String nombre,
                                         @RequestParam(required = false) MemberStatus estado) {
        return servicio.listar(nombre, estado).stream().map(RespuestaMiembro::desde).toList();
    }

    @GetMapping("/{id}")
    public RespuestaMiembro buscar(@PathVariable Long id) {
        return RespuestaMiembro.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaMiembro crear(@Valid @RequestBody SolicitudMiembro solicitud) {
        return RespuestaMiembro.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaMiembro actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudActualizarMiembro solicitud) {
        return RespuestaMiembro.desde(servicio.actualizar(id, solicitud));
    }

    @PatchMapping("/{id}/suspender")
    public RespuestaMiembro suspender(@PathVariable Long id) {
        return RespuestaMiembro.desde(servicio.suspender(id));
    }

    @PatchMapping("/{id}/activar")
    public RespuestaMiembro activar(@PathVariable Long id) {
        return RespuestaMiembro.desde(servicio.activar(id));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    @GetMapping("/{id}/consumo")
    public RespuestaConsumo consumo(@PathVariable Long id, @RequestParam(required = false) String mes) {
        return RespuestaConsumo.desde(servicioConsumo.consultar(id, mes));
    }
}
