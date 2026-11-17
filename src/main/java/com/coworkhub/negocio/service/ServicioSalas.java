package com.coworkhub.negocio.service;

import java.util.List;

import com.coworkhub.negocio.model.Room;
import com.coworkhub.presentacion.dto.SolicitudSala;

public interface ServicioSalas {

    /** Todas las salas, o solo las de una sede si sedeId no es null. */
    List<Room> listar(Long sedeId);

    Room buscar(Long id);

    Room crear(SolicitudSala solicitud);

    Room actualizar(Long id, SolicitudSala solicitud);

    void eliminar(Long id);
}
