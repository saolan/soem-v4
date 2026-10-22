package com.saolan.soem.configuracion.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.AsignacionAccesoDTO;
import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.configuracion.dto.SucuBodeDTO;
import com.saolan.soem.configuracion.dto.SucuCertEmisDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.dto.SucursalFiltroDTO;
import com.saolan.soem.configuracion.servicio.SucursalServicio;

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
 * Endpoints REST del agregado Sucursal (raiz {@code configuracion/sucursal}).
 * La composicion {@code certificadoEmisor} se administra como sub-recurso
 * propio bajo {@code /{id}/certificado-emision} (pantalla separada en el
 * frontend), no viaja embebida en {@link SucursalDTO}; igual las bodegas
 * habilitadas, bajo {@code /{id}/bodegas} (lectura y reemplazo en bloque).
 */
@Path("/sucursales")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SucursalControlador {

    // El permiso se controla a nivel de modulo (permisos 6000-6003).
    private static final String RECURSO = "configuracion-crud";

    @Inject
    private SucursalServicio sucursalServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("descri") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("codigo") String codigo,
            @QueryParam("descri") String descri,
            @QueryParam("ruc") String ruc,
            @QueryParam("ciudad") String ciudad,
            @QueryParam("matriz") Boolean matriz,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        SucursalFiltroDTO filtro = new SucursalFiltroDTO(codigo, descri, ruc, ciudad, matriz, estado);

        List<SucursalDTO> resultado = sucursalServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = sucursalServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(sucursalServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = sucursalServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid SucursalDTO sucursalDTO) {
        SucursalDTO creada = sucursalServicio.crear(sucursalDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creada, "Sucursal creada correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid SucursalDTO sucursalDTO) {
        SucursalDTO modificada = sucursalServicio.modificar(id, sucursalDTO);
        return Response.ok(RespuestaApi.exito(modificada, "Sucursal modificada correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        sucursalServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Sucursal eliminada correctamente")).build();
    }

    @GET
    @Path("/{id}/certificado-emision")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response obtenerCertificadoEmisor(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(sucursalServicio.obtenerCertificadoEmisor(id))).build();
    }

    @PUT
    @Path("/{id}/certificado-emision")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response guardarCertificadoEmisor(@PathParam("id") Integer id, @Valid SucuCertEmisDTO sucuCertEmisDTO) {
        SucuCertEmisDTO guardado = sucursalServicio.guardarCertificadoEmisor(id, sucuCertEmisDTO);
        return Response.ok(RespuestaApi.exito(guardado, "Certificado de firma electrónica guardado correctamente")).build();
    }

    @DELETE
    @Path("/{id}/certificado-emision")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminarCertificadoEmisor(@PathParam("id") Integer id) {
        sucursalServicio.eliminarCertificadoEmisor(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Certificado de firma electrónica eliminado correctamente")).build();
    }

    @GET
    @Path("/{id}/bodegas")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response bodegas(@PathParam("id") Integer id) {
        List<SucuBodeDTO> resultado = sucursalServicio.bodegas(id);
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @PUT
    @Path("/{id}/bodegas")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response reemplazarBodegas(@PathParam("id") Integer id, @Valid List<AsignacionAccesoDTO> asignaciones) {
        sucursalServicio.reemplazarBodegas(id, asignaciones);
        return Response.ok(RespuestaApi.<Void>exito(null, "Bodegas de la sucursal actualizadas correctamente")).build();
    }
}
