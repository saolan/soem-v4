package com.saolan.soem.compartido.excepcion;

/**
 * Credenciales invalidas o sesion no autenticada (login fallido, refresh
 * token invalido/revocado). Se traduce a HTTP 401. El mensaje debe ser
 * siempre generico: nunca distinguir "usuario no existe" de "clave
 * incorrecta", para no facilitar enumeracion de usuarios validos.
 */
public class ExcepcionNoAutenticado extends ExcepcionNegocio {

    private static final long serialVersionUID = 1L;

    public ExcepcionNoAutenticado(String mensaje) {
        super(mensaje);
    }
}
