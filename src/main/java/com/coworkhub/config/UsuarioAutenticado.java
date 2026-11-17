package com.coworkhub.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.coworkhub.negocio.model.Role;

/**
 * El usuario que inició sesión, tal como lo ve Spring Security. Lleva el miembroId para decidir
 * si un MEMBER es dueño de un recurso sin volver a consultar la base (null para ADMIN y RECEPTIONIST).
 */
public record UsuarioAutenticado(String username, String password, Role rol, Long miembroId) implements UserDetails {

    /** Copia sin contraseña: la que se guarda en el contexto de seguridad. */
    public UsuarioAutenticado sinContrasena() {
        return new UsuarioAutenticado(username, null, rol, miembroId);
    }

    public boolean esPersonal() {
        return rol == Role.ADMIN || rol == Role.RECEPTIONIST;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
