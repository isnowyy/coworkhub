package com.coworkhub.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403 con el cuerpo estándar cuando una regla por URL rechaza al usuario (por ejemplo, catálogos). */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final EscritorDeErrores escritor;

    public ManejadorAccesoDenegado(EscritorDeErrores escritor) {
        this.escritor = escritor;
    }

    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta, AccessDeniedException e)
            throws IOException {
        escritor.escribir(respuesta, HttpStatus.FORBIDDEN, "No tienes permiso para esta operación", "ACCESO_DENEGADO");
    }
}
