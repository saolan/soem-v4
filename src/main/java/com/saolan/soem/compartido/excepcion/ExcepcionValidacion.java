package com.saolan.soem.compartido.excepcion;

import java.util.List;

import com.saolan.soem.compartido.dto.ErrorCampo;

/**
 * Error de validacion de negocio (no cubierto por Bean Validation). Se
 * traduce a HTTP 400.
 */
public class ExcepcionValidacion extends ExcepcionNegocio {

    private static final long serialVersionUID = 1L;

    public ExcepcionValidacion(String mensaje) {
        super(mensaje);
    }

    public ExcepcionValidacion(String mensaje, List<ErrorCampo> errores) {
        super(mensaje, errores);
    }
}
