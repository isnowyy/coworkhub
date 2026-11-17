package com.coworkhub.negocio.service;

import com.coworkhub.config.UsuarioAutenticado;

/** Valida las credenciales del login (RF-11). */
public interface ServicioAutenticacion {

    /** @throws com.coworkhub.negocio.excepcion.CredencialesInvalidasException si no coinciden */
    UsuarioAutenticado autenticar(String usuario, String contrasena);
}
