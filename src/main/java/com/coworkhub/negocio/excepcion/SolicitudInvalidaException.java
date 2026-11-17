package com.coworkhub.negocio.excepcion;

/**
 * Los datos tienen un formato válido por separado, pero juntos no tienen sentido (por ejemplo,
 * una hora de cierre anterior a la de apertura, o un listado sin filtro). Se traduce a 400.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
