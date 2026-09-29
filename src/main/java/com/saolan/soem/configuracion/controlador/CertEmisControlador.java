package com.saolan.soem.configuracion.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.configuracion.servicio.CertEmisServicio;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Combo de solo lectura del catalogo externo CERT_EMIS, dueño el modulo
 * configuracion (unico consumidor: {@code SUCU_CERT_EMIS.CERT_EMIS_ID}).
 * No expone CRUD: eso queda fuera del alcance de este agregado.
 */
@Path("/certificados-emisor")
@Produces(MediaType.APPLICATION_JSON)
public class CertEmisControlador {

    @Inject
    private CertEmisServicio certEmisServicio;

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = "certificados-emisor", accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = certEmisServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }
}
