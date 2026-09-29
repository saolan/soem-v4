package com.saolan.soem.compartido.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * Tests unitarios de {@link FiltroAutorizacion}: mockea {@link ResourceInfo}
 * (usando métodos reales, obtenidos por reflexión, de clases de prueba
 * anotadas con {@link RequierePermiso}), {@link JsonWebToken} y
 * {@link ContainerRequestContext} — sin necesidad de un contenedor JAX-RS real.
 */
@ExtendWith(MockitoExtension.class)
class FiltroAutorizacionTest {

    private static final String CLAIM_PERMISOS = "permisos";

    @Mock
    private JsonWebToken jsonWebToken;

    @Mock
    private ResourceInfo resourceInfo;

    @Mock
    private ContainerRequestContext requestContext;

    @InjectMocks
    private FiltroAutorizacion filtro;

    @Test
    void filter_deberiaDejarPasar_cuandoElMetodoNoTieneRequierePermiso() throws Exception {
        stubRecurso(metodo("metodoSinPermiso"), RecursoDePrueba.class);

        filtro.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_deberiaRetornar401_cuandoNoHayTokenAutenticado() throws Exception {
        stubRecurso(metodo("metodoConPermiso"), RecursoDePrueba.class);
        // jsonWebToken.getRawToken() no stubeado -> null por defecto (mock sin token real)

        filtro.filter(requestContext);

        assertEstadoAbortado(401);
    }

    @Test
    void filter_deberiaRetornar403_cuandoElTokenNoTieneElPermisoRequerido() throws Exception {
        stubRecurso(metodo("metodoConPermiso"), RecursoDePrueba.class);
        when(jsonWebToken.getRawToken()).thenReturn("token-valido");
        when(jsonWebToken.getClaim(CLAIM_PERMISOS)).thenReturn(arregloDePermisos("otros:buscar"));

        filtro.filter(requestContext);

        assertEstadoAbortado(403);
    }

    @Test
    void filter_deberiaDejarPasar_cuandoElTokenTieneElPermisoExacto() throws Exception {
        stubRecurso(metodo("metodoConPermiso"), RecursoDePrueba.class);
        when(jsonWebToken.getRawToken()).thenReturn("token-valido");
        when(jsonWebToken.getClaim(CLAIM_PERMISOS)).thenReturn(arregloDePermisos("sucursales:crear"));

        filtro.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_deberiaUsarLaAnotacionDeClase_cuandoElMetodoNoTieneUnaPropia() throws Exception {
        stubRecurso(RecursoConPermisoEnClase.class.getMethod("metodoSinAnotacionPropia"), RecursoConPermisoEnClase.class);
        when(jsonWebToken.getRawToken()).thenReturn("token-valido");
        when(jsonWebToken.getClaim(CLAIM_PERMISOS)).thenReturn(arregloDePermisos("otro-recurso:buscar"));

        filtro.filter(requestContext);

        // La anotación de RecursoConPermisoEnClase exige "roles:buscar": el permiso
        // del claim no coincide, así que debe rechazar (confirma que sí se leyó del tipo).
        assertEstadoAbortado(403);
    }

    private void stubRecurso(Method metodoAnotado, Class<?> clase) {
        when(resourceInfo.getResourceMethod()).thenReturn(metodoAnotado);
        if (metodoAnotado.getAnnotation(RequierePermiso.class) == null) {
            doReturn(clase).when(resourceInfo).getResourceClass();
        }
    }

    private void assertEstadoAbortado(int estadoEsperado) {
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(captor.capture());
        assertEquals(estadoEsperado, captor.getValue().getStatus());
    }

    private JsonArray arregloDePermisos(String... permisos) {
        var builder = Json.createArrayBuilder();
        for (String permiso : permisos) {
            builder.add(permiso);
        }
        return builder.build();
    }

    private Method metodo(String nombre) throws NoSuchMethodException {
        return RecursoDePrueba.class.getMethod(nombre);
    }

    private static class RecursoDePrueba {
        @RequierePermiso(recurso = "sucursales", accion = Accion.CREAR)
        public void metodoConPermiso() {
        }

        public void metodoSinPermiso() {
        }
    }

    @RequierePermiso(recurso = "roles", accion = Accion.BUSCAR)
    private static class RecursoConPermisoEnClase {
        public void metodoSinAnotacionPropia() {
        }
    }
}
