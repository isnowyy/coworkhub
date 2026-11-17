package com.coworkhub.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.coworkhub.presentacion.dto.RespuestaError;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe el cuerpo de error estándar desde los filtros de seguridad, que se ejecutan antes de
 * llegar a los controladores (y por eso no pasan por el @RestControllerAdvice).
 */
@Component
public class EscritorDeErrores {

    private final JsonMapper json;

    public EscritorDeErrores(JsonMapper json) {
        this.json = json;
    }

    public void escribir(HttpServletResponse respuesta, HttpStatus estado, String mensaje, String codigo)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getOutputStream(), RespuestaError.de(mensaje, codigo));
    }
}
