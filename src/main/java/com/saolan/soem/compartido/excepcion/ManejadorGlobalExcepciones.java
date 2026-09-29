package com.saolan.soem.compartido.excepcion;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.RespuestaApi;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Manejo centralizado de excepciones para todos los endpoints REST. Traduce
 * cualquier excepcion a la envoltura {@link RespuestaApi}, sin exponer
 * jamas trazas, mensajes tecnicos ni consultas SQL al cliente.
 */
@Provider
public class ManejadorGlobalExcepciones implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(ManejadorGlobalExcepciones.class.getName());
    private static final String MENSAJE_ERROR_INTERNO =
            "Ocurrió un error interno. Contacte al administrador del sistema.";

    @Override
    public Response toResponse(Throwable excepcion) {
        if (excepcion instanceof ExcepcionValidacion excepcionValidacion) {
            return construir(Response.Status.BAD_REQUEST, excepcionValidacion.getMessage(), excepcionValidacion.getErrores());
        }
        if (excepcion instanceof ExcepcionNoEncontrado excepcionNoEncontrado) {
            return construir(Response.Status.NOT_FOUND, excepcionNoEncontrado.getMessage(), excepcionNoEncontrado.getErrores());
        }
        if (excepcion instanceof ExcepcionConflicto excepcionConflicto) {
            return construir(Response.Status.CONFLICT, excepcionConflicto.getMessage(), excepcionConflicto.getErrores());
        }
        if (excepcion instanceof ExcepcionNoAutenticado excepcionNoAutenticado) {
            return construir(Response.Status.UNAUTHORIZED, excepcionNoAutenticado.getMessage(), excepcionNoAutenticado.getErrores());
        }
        if (excepcion instanceof ExcepcionNegocio excepcionNegocio) {
            return construir(Response.Status.BAD_REQUEST, excepcionNegocio.getMessage(), excepcionNegocio.getErrores());
        }
        if (excepcion instanceof ConstraintViolationException constraintViolationException) {
            return construirDesdeViolaciones(constraintViolationException);
        }
        if (excepcion instanceof WebApplicationException webApplicationException) {
            int estado = webApplicationException.getResponse().getStatus();
            return construir(Response.Status.fromStatusCode(estado), webApplicationException.getMessage(), List.of());
        }

        LOGGER.log(Level.SEVERE, "Error no controlado al procesar la solicitud", excepcion);
        return construir(Response.Status.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_INTERNO, List.of());
    }

    private Response construirDesdeViolaciones(ConstraintViolationException excepcion) {
        List<ErrorCampo> errores = excepcion.getConstraintViolations().stream()
                .map(violacion -> new ErrorCampo(nombreCampo(violacion.getPropertyPath().toString()), violacion.getMessage()))
                .toList();
        return construir(Response.Status.BAD_REQUEST, "Error de validación en los datos enviados", errores);
    }

    private String nombreCampo(String rutaPropiedad) {
        int indice = rutaPropiedad.lastIndexOf('.');
        return indice >= 0 ? rutaPropiedad.substring(indice + 1) : rutaPropiedad;
    }

    private Response construir(Response.Status estado, String mensaje, List<ErrorCampo> errores) {
        return Response.status(estado)
                .type(MediaType.APPLICATION_JSON)
                .entity(RespuestaApi.error(mensaje, errores))
                .build();
    }
}
