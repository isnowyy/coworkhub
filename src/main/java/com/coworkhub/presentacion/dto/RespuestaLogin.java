package com.coworkhub.presentacion.dto;

import java.time.Instant;

import com.coworkhub.negocio.model.Role;

/** Token para enviar en "Authorization: Bearer &lt;token&gt;", con su vencimiento y quién es. */
public record RespuestaLogin(String token, String tipo, Instant expiraEn, String usuario, Role rol, Long miembroId) {
}
