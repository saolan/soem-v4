package com.saolan.soem.compartido.excepcion;

/**
 * Texto recibido en un campo decimal del JSON que no es un numero valido.
 * La lanza {@code DecimalTextoAdaptador} durante la lectura del cuerpo; llega
 * envuelta por JSON-B y RESTEasy, y {@link ManejadorGlobalExcepciones} la
 * busca en la cadena de causas para responder 400 en lugar de 500.
 * <p>
 * No guarda el valor rechazado: nunca se devuelve ni se registra.
 */
public class ExcepcionDecimalInvalido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExcepcionDecimalInvalido() {
        super("Valor decimal no válido", null, false, false);
    }
}
