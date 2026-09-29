package com.saolan.soem.compartido.seguridad;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.saolan.soem.compartido.dto.RespuestaApi;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Autorizacion fina por permiso, complementaria a la autenticacion MP-JWT del
 * contenedor (que ya valida firma y expiracion del token antes de que este
 * filtro se ejecute). Verifica que el claim {@code permisos} del JWT
 * autenticado contenga la cadena {@code recurso:accion} declarada por
 * {@link RequierePermiso} en el endpoint invocado.
 * <p>
 * Los permisos se calculan una unica vez al emitir el token -responsabilidad
 * del futuro modulo {@code seguridad}, que los obtiene desde la base de
 * datos- y viajan embebidos como claim, evitando una consulta por cada
 * solicitud.
 * <p>
 * La anotacion {@code @RequierePermiso} aplicada aqui con valores fijos solo
 * sirve para el name-binding de JAX-RS (que compara por tipo de anotacion,
 * no por sus atributos); el recurso y la accion reales de cada solicitud se
 * leen en tiempo de ejecucion desde el metodo invocado via {@link ResourceInfo}.
 */
@Provider
@RequierePermiso(recurso = "*", accion = Accion.BUSCAR)
@Priority(Priorities.AUTHORIZATION)
public class FiltroAutorizacion implements ContainerRequestFilter {

    private static final String CLAIM_PERMISOS = "permisos";

    @Inject
    private JsonWebToken jsonWebToken;

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RequierePermiso requierePermiso = resourceInfo.getResourceMethod().getAnnotation(RequierePermiso.class);
        if (requierePermiso == null) {
            requierePermiso = resourceInfo.getResourceClass().getAnnotation(RequierePermiso.class);
        }
        if (requierePermiso == null) {
            return;
        }

        if (jsonWebToken == null || jsonWebToken.getRawToken() == null) {
            abortar(requestContext, Response.Status.UNAUTHORIZED, "Debe autenticarse para acceder a este recurso");
            return;
        }

        String permisoRequerido = construirPermiso(requierePermiso.recurso(), requierePermiso.accion());
        Set<String> permisos = UtilidadClaims.comoConjuntoDeTexto(jsonWebToken.getClaim(CLAIM_PERMISOS));
        if (!permisos.contains(permisoRequerido)) {
            abortar(requestContext, Response.Status.FORBIDDEN, "No tiene permiso para realizar esta acción");
        }
    }

    private String construirPermiso(String recurso, Accion accion) {
        return recurso + ":" + accion.name().toLowerCase(Locale.ROOT);
    }

    private void abortar(ContainerRequestContext requestContext, Response.Status estado, String mensaje) {
        requestContext.abortWith(Response.status(estado)
                .type(MediaType.APPLICATION_JSON)
                .entity(RespuestaApi.error(mensaje))
                .build());
    }
}
