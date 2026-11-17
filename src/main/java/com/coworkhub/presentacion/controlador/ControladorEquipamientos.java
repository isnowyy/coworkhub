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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/equipamientos")
public class ControladorEquipamientos {

    private final ServicioEquipamientos servicio;

    public ControladorEquipamientos(ServicioEquipamientos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaEquipamiento> listar() {
        return servicio.listar().stream().map(RespuestaEquipamiento::desde).toList();
    }

    @GetMapping("/{id}")
    public RespuestaEquipamiento buscar(@PathVariable Long id) {
        return RespuestaEquipamiento.desde(servicio.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaEquipamiento crear(@Valid @RequestBody SolicitudEquipamiento solicitud) {
        return RespuestaEquipamiento.desde(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    public RespuestaEquipamiento actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudEquipamiento solicitud) {
        return RespuestaEquipamiento.desde(servicio.actualizar(id, solicitud));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
