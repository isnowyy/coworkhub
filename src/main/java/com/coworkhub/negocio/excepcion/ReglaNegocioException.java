package com.coworkhub.negocio.excepcion;

/**
 * Se violó una regla de negocio o hay un duplicado. Lleva el código de la regla (RN-01…)
 * para el cuerpo de error. El manejador global la traduce a 409.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
