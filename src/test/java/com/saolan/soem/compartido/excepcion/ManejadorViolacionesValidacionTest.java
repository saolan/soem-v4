package com.saolan.soem.compartido.excepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jboss.resteasy.api.validation.ConstraintType;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.junit.jupiter.api.Test;

import com.saolan.soem.compartido.dto.RespuestaApi;

import jakarta.ws.rs.core.Response;

/**
 * Tests unitarios de {@link ManejadorViolacionesValidacion}: la violación de
 * un parámetro {@code @Valid} se traduce a {@link RespuestaApi} con el
 * nombre del campo y sin el valor rechazado.
 */
class ManejadorViolacionesValidacionTest {

    private final ManejadorViolacionesValidacion manejador = new ManejadorViolacionesValidacion();

    @Test
    void toResponse_deberiaRetornar400ConCampoYSinElValorEnviado() {
        ResteasyViolationException excepcion = mock(ResteasyViolationException.class);
        when(excepcion.getReturnValueViolations()).thenReturn(List.of());
        when(excepcion.getViolations()).thenReturn(List.of(
                violacion("login.arg0.clave", "La clave es obligatoria", "secreta-123"),
                violacion("login.arg0.usuario", "El usuario es obligatorio", "")));

        Response respuesta = manejador.toResponse(excepcion);

        assertEquals(400, respuesta.getStatus());
        RespuestaApi<?> cuerpo = (RespuestaApi<?>) respuesta.getEntity();
        assertFalse(cuerpo.exito());
        assertEquals(2, cuerpo.errores().size());
        assertEquals("clave", cuerpo.errores().get(0).campo());
        assertEquals("La clave es obligatoria", cuerpo.errores().get(0).detalle());
        assertFalse(cuerpo.toString().contains("secreta-123"));
    }

    @Test
    void toResponse_deberiaRetornar500_cuandoFallaElValorDeRetorno() {
        ResteasyViolationException excepcion = mock(ResteasyViolationException.class);
        when(excepcion.getReturnValueViolations()).thenReturn(List.of(
                violacion("buscar.<return value>", "no debe ser nulo", "null")));

        Response respuesta = manejador.toResponse(excepcion);

        assertEquals(500, respuesta.getStatus());
        assertTrue(((RespuestaApi<?>) respuesta.getEntity()).errores().isEmpty());
    }

    private ResteasyConstraintViolation violacion(String ruta, String mensaje, String valor) {
        return new ResteasyConstraintViolation(ConstraintType.Type.PARAMETER, ruta, mensaje, valor);
    }
}
