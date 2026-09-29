package com.saolan.soem.compartido.excepcion;

/**
 * El recurso solicitado no existe. Se traduce a HTTP 404.
 */
public class ExcepcionNoEncontrado extends ExcepcionNegocio {

    private static final long serialVersionUID = 1L;

    public ExcepcionNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
