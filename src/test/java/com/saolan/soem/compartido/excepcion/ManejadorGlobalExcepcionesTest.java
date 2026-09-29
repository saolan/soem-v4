package com.saolan.soem.compartido.excepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.RespuestaApi;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Tests unitarios de {@link ManejadorGlobalExcepciones}: se instancia
 * directo (no es un componente CDI con estado) y se invoca
 * {@link ManejadorGlobalExcepciones#toResponse(Throwable)} con cada tipo de
 * excepción soportado, verificando el código HTTP y que nunca se filtre un
 * mensaje técnico al cliente.
 */
class ManejadorGlobalExcepcionesTest {

    private static final String MENSAJE_ERROR_INTERNO = "Ocurrió un error interno. Contacte al administrador del sistema.";

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    @Test
    void toResponse_deberiaRetornar400_paraExcepcionValidacion() {
        ExcepcionValidacion excepcion = new ExcepcionValidacion("dato inválido",
                List.of(new ErrorCampo("campo", "detalle")));

        Response respuesta = manejador.toResponse(excepcion);

        assertEquals(400, respuesta.getStatus());
        assertCuerpo(respuesta, "dato inválido", 1);
    }

    @Test
    void toResponse_deberiaRetornar404_paraExcepcionNoEncontrado() {
        Response respuesta = manejador.toResponse(new ExcepcionNoEncontrado("no existe"));

        assertEquals(404, respuesta.getStatus());
        assertCuerpo(respuesta, "no existe", 0);
    }

    @Test
    void toResponse_deberiaRetornar409_paraExcepcionConflicto() {
        Response respuesta = manejador.toResponse(new ExcepcionConflicto("ya existe"));

        assertEquals(409, respuesta.getStatus());
        assertCuerpo(respuesta, "ya existe", 0);
    }

    @Test
    void toResponse_deberiaRetornar401_paraExcepcionNoAutenticado() {
        Response respuesta = manejador.toResponse(new ExcepcionNoAutenticado("credenciales inválidas"));

        assertEquals(401, respuesta.getStatus());
        assertCuerpo(respuesta, "credenciales inválidas", 0);
    }

    @Test
    void toResponse_deberiaRetornar400_paraExcepcionNegocioGenerica() {
        Response respuesta = manejador.toResponse(new ExcepcionNegocio("regla de negocio violada"));

        assertEquals(400, respuesta.getStatus());
        assertCuerpo(respuesta, "regla de negocio violada", 0);
    }

    @Test
    void toResponse_deberiaExtraerNombreDeCampo_paraConstraintViolationException() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violacion = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("crear.arg0.codigo");
        when(violacion.getPropertyPath()).thenReturn(path);
        when(violacion.getMessage()).thenReturn("El código es obligatorio");

        Response respuesta = manejador.toResponse(new ConstraintViolationException(Set.of(violacion)));

        assertEquals(400, respuesta.getStatus());
        RespuestaApi<?> cuerpo = (RespuestaApi<?>) respuesta.getEntity();
        assertEquals(1, cuerpo.errores().size());
        assertEquals("codigo", cuerpo.errores().get(0).campo());
        assertEquals("El código es obligatorio", cuerpo.errores().get(0).detalle());
    }

    @Test
    void toResponse_deberiaPropagarElStatus_paraWebApplicationException() {
        Response respuesta = manejador.toResponse(new WebApplicationException("no autorizado", Response.Status.FORBIDDEN));

        assertEquals(403, respuesta.getStatus());
    }

    @Test
    void toResponse_deberiaRetornar500SinExponerElMensajeTecnico_paraExcepcionNoControlada() {
        Response respuesta = manejador.toResponse(new RuntimeException("NullPointerException en la línea 42, tabla sucursal, columna x"));

        assertEquals(500, respuesta.getStatus());
        RespuestaApi<?> cuerpo = (RespuestaApi<?>) respuesta.getEntity();
        assertEquals(MENSAJE_ERROR_INTERNO, cuerpo.mensaje());
        assertNotEquals("NullPointerException en la línea 42, tabla sucursal, columna x", cuerpo.mensaje());
        assertTrue(cuerpo.errores().isEmpty());
    }

    private void assertCuerpo(Response respuesta, String mensajeEsperado, int cantidadErrores) {
        RespuestaApi<?> cuerpo = (RespuestaApi<?>) respuesta.getEntity();
        assertEquals(mensajeEsperado, cuerpo.mensaje());
        assertEquals(cantidadErrores, cuerpo.errores().size());
        assertEquals(false, cuerpo.exito());
    }
}
