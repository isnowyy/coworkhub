package com.coworkhub.negocio.service;

import java.util.List;

import com.coworkhub.negocio.model.AdditionalService;
import com.coworkhub.presentacion.dto.SolicitudServicioAdicional;

public interface ServicioServiciosAdicionales {

    List<AdditionalService> listar();

    AdditionalService buscar(Long id);

    AdditionalService crear(SolicitudServicioAdicional solicitud);

    AdditionalService actualizar(Long id, SolicitudServicioAdicional solicitud);

    void eliminar(Long id);
}
