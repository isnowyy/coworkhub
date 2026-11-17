package com.coworkhub.negocio.service;

import java.util.List;

import com.coworkhub.negocio.model.MembershipPlan;
import com.coworkhub.presentacion.dto.SolicitudPlan;

public interface ServicioPlanes {

    List<MembershipPlan> listar();

    MembershipPlan buscar(Long id);

    MembershipPlan crear(SolicitudPlan solicitud);

    MembershipPlan actualizar(Long id, SolicitudPlan solicitud);

    void eliminar(Long id);
}
