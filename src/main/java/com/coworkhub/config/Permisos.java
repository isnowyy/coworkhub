package com.coworkhub.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.coworkhub.negocio.service.ServicioReservas;

/**
 * Reglas de "dueño del recurso" (RN-13) para usar en @PreAuthorize, por ejemplo
 * {@code @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST') or @permisos.esSuReserva(#id)")}.
 * Lo que depende de quién es el dueño no se puede decidir solo por URL.
 */
@Component("permisos")
public class Permisos {

    private final ServicioReservas servicioReservas;

    public Permisos(ServicioReservas servicioReservas) {
        this.servicioReservas = servicioReservas;
    }

    /** El usuario es un MEMBER y el miembro pedido es él mismo. */
    public boolean esSuMiembro(Long miembroId) {
        Long propio = miembroActual();
        return propio != null && propio.equals(miembroId);
    }

    /**
     * El usuario es un MEMBER y la reserva es suya. Si la reserva no existe, también es false:
     * el miembro recibe 403 y no puede averiguar qué ids existen.
     */
    public boolean esSuReserva(Long reservaId) {
        Long propio = miembroActual();
        return propio != null && reservaId != null && servicioReservas.perteneceA(reservaId, propio);
    }

    private static Long miembroActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario.miembroId();
        }
        return null;
    }
}
