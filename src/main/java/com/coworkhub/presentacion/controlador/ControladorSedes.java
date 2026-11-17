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

import com.coworkhub.negocio.service.ServicioSedes;
import com.coworkhub.presentacion.dto.SolicitudSede;
import com.coworkhub.presentacion.dto.RespuestaSede;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sedes")
public class ControladorSedes {

    private final ServicioSedes servicio;

    public ControladorSedes(ServicioSedes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaSede> listar() {
        return servicio.listar().stream().map(RespuestaSede::desde).toList();
    }

    @GetMapping("/{id}")
    public RespuestaSede buscar(@PathVariable Long id) {
        return RespuestaSede.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaSede crear(@Valid @RequestBody SolicitudSede solicitud) {
        return RespuestaSede.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaSede actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudSede solicitud) {
        return RespuestaSede.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
