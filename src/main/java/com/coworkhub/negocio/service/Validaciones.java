package com.coworkhub.negocio.service;

import com.coworkhub.negocio.excepcion.ReglaNegocioException;

/** Comprobaciones repetidas en varios servicios, en un solo lugar. */
final class Validaciones {

    private Validaciones() {
    }

    /** RN-11: lanza 409 si ya existe otro registro con ese valor. */
    static void exigirUnico(boolean yaExiste, String mensaje) {
        if (yaExiste) {
            throw new ReglaNegocioException("RN-11", mensaje);
        }
    }

    /** RN-12: lanza 409 si el recurso está en uso. */
    static void exigirSinUso(boolean enUso, String mensaje) {
        if (enUso) {
            throw new ReglaNegocioException("RN-12", mensaje);
        }
    }

    /** Quita los espacios de los extremos (los nombres se comparan y guardan sin ellos). */
    static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
