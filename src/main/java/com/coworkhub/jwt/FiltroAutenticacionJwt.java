package com.coworkhub.jwt;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autenticación stateless: en cada solicitud lee "Authorization: Bearer &lt;token&gt;", lo valida y,
 * si es correcto, deja al usuario en el contexto de seguridad. Si falta o es inválido no autentica,
 * y Spring Security responde 401 en las rutas protegidas. No es @Component: se registra solo dentro
 * de la cadena de seguridad, para que no se ejecute dos veces.
 */
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final UtilJwt utilJwt;

    public FiltroAutenticacionJwt(UtilJwt utilJwt) {
        this.utilJwt = utilJwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String encabezado = solicitud.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            utilJwt.validar(encabezado.substring(PREFIJO.length()).trim()).ifPresent(usuario -> {
                var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            });
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
