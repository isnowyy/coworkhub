package com.coworkhub.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 401 con el cuerpo estándar: sin credenciales, o con un token alterado o vencido. */
@Component
public class PuntoEntradaNoAutenticado implements AuthenticationEntryPoint {

    private final EscritorDeErrores escritor;

    public PuntoEntradaNoAutenticado(EscritorDeErrores escritor) {
        this.escritor = escritor;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta, AuthenticationException e)
            throws IOException {
        escritor.escribir(respuesta, HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesión: falta el token, está alterado o ya venció", "NO_AUTENTICADO");
    }
}
