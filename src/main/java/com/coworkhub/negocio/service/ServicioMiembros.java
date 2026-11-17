package com.coworkhub.negocio.service;

import java.util.List;

import com.coworkhub.negocio.model.Member;
import com.coworkhub.negocio.model.MemberStatus;
import com.coworkhub.presentacion.dto.SolicitudActualizarMiembro;
import com.coworkhub.presentacion.dto.SolicitudMiembro;

public interface ServicioMiembros {

    /** Filtros opcionales: null = sin filtro. */
    List<Member> listar(String nombre, MemberStatus estado);

    Member buscar(Long id);

    /** Crea el miembro y su cuenta (rol MEMBER) en una sola transacción. */
    Member crear(SolicitudMiembro solicitud);

    Member actualizar(Long id, SolicitudActualizarMiembro solicitud);

    Member suspender(Long id);

    Member activar(Long id);

    void eliminar(Long id);
}
