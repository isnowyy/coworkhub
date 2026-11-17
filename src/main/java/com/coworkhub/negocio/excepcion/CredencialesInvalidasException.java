package com.coworkhub.negocio.excepcion;

/** Usuario o contraseña incorrectos en el login. Se traduce a 401. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos");
    }
}
