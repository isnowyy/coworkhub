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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/planes")
public class ControladorPlanes {

    private final ServicioPlanes servicio;

    public ControladorPlanes(ServicioPlanes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaPlan> listar() {
        return servicio.listar().stream().map(RespuestaPlan::desde).toList();
    }

    @GetMapping("/{id}")
    public RespuestaPlan buscar(@PathVariable Long id) {
        return RespuestaPlan.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaPlan crear(@Valid @RequestBody SolicitudPlan solicitud) {
        return RespuestaPlan.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaPlan actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudPlan solicitud) {
        return RespuestaPlan.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
