package com.saolan.soem.compartido.excepcion;

import java.util.List;

import com.saolan.soem.compartido.dto.ErrorCampo;

/**
 * Excepcion base para errores de negocio previstos (no fallos tecnicos). El
 * {@link com.saolan.soem.compartido.excepcion.ManejadorGlobalExcepciones} la
 * traduce directamente al contrato {@link com.saolan.soem.compartido.dto.RespuestaApi}.
 */
public class ExcepcionNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<ErrorCampo> errores;

    public ExcepcionNegocio(String mensaje) {
        super(mensaje);
        this.errores = List.of();
    }

    public ExcepcionNegocio(String mensaje, List<ErrorCampo> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
