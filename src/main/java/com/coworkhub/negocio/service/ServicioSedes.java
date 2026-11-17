package com.coworkhub.negocio.service;

import java.util.List;

import com.coworkhub.negocio.model.Location;
import com.coworkhub.presentacion.dto.SolicitudSede;

public interface ServicioSedes {

    List<Location> listar();

    Location buscar(Long id);

    Location crear(SolicitudSede solicitud);

    Location actualizar(Long id, SolicitudSede solicitud);

    void eliminar(Long id);
}
