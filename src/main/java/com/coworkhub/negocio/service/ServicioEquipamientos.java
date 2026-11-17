package com.coworkhub.negocio.service;

import java.util.List;

import com.coworkhub.negocio.model.Equipment;
import com.coworkhub.presentacion.dto.SolicitudEquipamiento;

public interface ServicioEquipamientos {

    List<Equipment> listar();

    Equipment buscar(Long id);

    Equipment crear(SolicitudEquipamiento solicitud);

    Equipment actualizar(Long id, SolicitudEquipamiento solicitud);

    void eliminar(Long id);
}
