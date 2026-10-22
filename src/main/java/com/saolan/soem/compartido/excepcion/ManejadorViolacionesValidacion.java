package com.saolan.soem.compartido.excepcion;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.resteasy.api.validation.ResteasyViolationException;

import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.RespuestaApi;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduce las violaciones de Bean Validation de los parametros
 * {@code @Valid} a la envoltura {@link RespuestaApi}. RESTEasy las lanza como
 * {@link ResteasyViolationException} y trae su propio mapper para ese tipo
 * exacto, que gana sobre {@link ManejadorGlobalExcepciones} (registrado para
 * {@code Throwable}) y responde con su formato, incluido el valor enviado
 * (p. ej. la clave del login). Un mapper de la aplicacion para el mismo tipo
 * reemplaza al incorporado.
 * <p>
 * Nunca devuelve ni registra el valor rechazado: solo el campo y el mensaje.
 */
@Provider
public class ManejadorViolacionesValidacion implements ExceptionMapper<ResteasyViolationException> {

    private static final Logger LOGGER = Logger.getLogger(ManejadorViolacionesValidacion.class.getName());

    @Override
    public Response toResponse(ResteasyViolationException excepcion) {
        // Fallo del propio validador o del valor de retorno: es un error del servidor, no del cliente.
        if (excepcion.getException() != null || !excepcion.getReturnValueViolations().isEmpty()) {
            LOGGER.log(Level.SEVERE, "Error de validación en el servidor: {0} violaciones en el valor de retorno",
                    excepcion.getReturnValueViolations().size());
            return ManejadorGlobalExcepciones.respuestaErrorInterno();
        }

        List<ErrorCampo> errores = excepcion.getViolations().stream()
                .map(violacion -> new ErrorCampo(
                        ManejadorGlobalExcepciones.nombreCampo(Objects.toString(violacion.getPath(), "")),
                        violacion.getMessage()))
                .toList();
        return ManejadorGlobalExcepciones.respuestaDeValidacion(errores);
    }
}
