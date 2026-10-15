package com.saolan.soem.configuracion.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.configuracion.dto.CertEmisDTO;
import com.saolan.soem.configuracion.dto.CertEmisFiltroDTO;
import com.saolan.soem.configuracion.servicio.CertEmisServicio;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoints REST del catalogo CERT_EMIS (emisores de certificados de firma
 * electronica). El combo lo consume la pantalla de certificado de emision
 * de la sucursal ({@code SUCU_CERT_EMIS.CERT_EMIS_ID}).
 */
@Path("/certificados-emisor")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CertEmisControlador {

    // El permiso se controla a nivel de modulo (permisos 6000-6003).
    private static final String RECURSO = "configuracion-crud";

    @Inject
    private CertEmisServicio certEmisServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("descri") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("descri") String descri,
            @QueryParam("certCn") String certCn,
            @QueryParam("certO") String certO,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        CertEmisFiltroDTO filtro = new CertEmisFiltroDTO(descri, certCn, certO, estado);

        List<CertEmisDTO> resultado = certEmisServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = certEmisServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(certEmisServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = certEmisServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid CertEmisDTO certEmisDTO) {
        CertEmisDTO creado = certEmisServicio.crear(certEmisDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creado, "Certificado emisor creado correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid CertEmisDTO certEmisDTO) {
        CertEmisDTO modificado = certEmisServicio.modificar(id, certEmisDTO);
        return Response.ok(RespuestaApi.exito(modificado, "Certificado emisor modificado correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        certEmisServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Certificado emisor eliminado correctamente")).build();
    }
}
