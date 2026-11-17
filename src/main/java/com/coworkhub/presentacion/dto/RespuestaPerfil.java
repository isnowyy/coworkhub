package com.coworkhub.presentacion.dto;

import com.coworkhub.config.UsuarioAutenticado;
import com.coworkhub.negocio.model.Role;

/** Datos del usuario que inició sesión (RF-14). miembroId es null para ADMIN y RECEPTIONIST. */
public record RespuestaPerfil(String usuario, Role rol, Long miembroId) {

    public static RespuestaPerfil desde(UsuarioAutenticado u) {
        return new RespuestaPerfil(u.username(), u.rol(), u.miembroId());
    }
}
