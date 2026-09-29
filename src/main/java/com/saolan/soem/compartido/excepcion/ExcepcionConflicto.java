package com.saolan.soem.compartido.excepcion;

import java.util.List;

import com.saolan.soem.compartido.dto.ErrorCampo;

/**
 * El request entra en conflicto con el estado actual de los datos
 * (violacion de una regla de unicidad o de una regla de negocio similar).
 * Se traduce a HTTP 409.
 */
public class ExcepcionConflicto extends ExcepcionNegocio {

    private static final long serialVersionUID = 1L;

    public ExcepcionConflicto(String mensaje) {
        super(mensaje);
    }

    public ExcepcionConflicto(String mensaje, List<ErrorCampo> errores) {
        super(mensaje, errores);
    }
}
