package com.coworkhub.negocio.excepcion;

/** El recurso pedido no existe. El manejador global la traduce a 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " con id " + id + " no existe");
    }
}
