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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios-adicionales")
public class ControladorServiciosAdicionales {

    private final ServicioServiciosAdicionales servicio;

    public ControladorServiciosAdicionales(ServicioServiciosAdicionales servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaServicioAdicional> listar() {
        return servicio.listar().stream().map(RespuestaServicioAdicional::desde).toList();
    }

    @GetMapping("/{id}")
    public RespuestaServicioAdicional buscar(@PathVariable Long id) {
        return RespuestaServicioAdicional.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaServicioAdicional crear(@Valid @RequestBody SolicitudServicioAdicional solicitud) {
        return RespuestaServicioAdicional.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaServicioAdicional actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudServicioAdicional solicitud) {
        return RespuestaServicioAdicional.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
