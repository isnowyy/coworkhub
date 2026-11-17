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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salas")
public class ControladorSalas {

    private final ServicioSalas servicio;

    public ControladorSalas(ServicioSalas servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaSala> listar(@RequestParam(required = false) Long sedeId) {
        return servicio.listar(sedeId).stream().map(RespuestaSala::desde).toList();
    }

    @GetMapping("/{id}")
    public RespuestaSala buscar(@PathVariable Long id) {
        return RespuestaSala.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaSala crear(@Valid @RequestBody SolicitudSala solicitud) {
        return RespuestaSala.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaSala actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudSala solicitud) {
        return RespuestaSala.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
