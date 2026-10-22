package com.saolan.soem.configuracion.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.configuracion.dto.BodegaDTO;
import com.saolan.soem.configuracion.dto.BodegaFiltroDTO;
import com.saolan.soem.configuracion.servicio.BodegaServicio;

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
 * Endpoints REST del catalogo BODEGA. El combo lo consumen la asignacion
 * por sucursal ({@code /sucursales/{id}/bodegas}) y los modulos de
 * inventario, compras y ventas.
 */
@Path("/bodegas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BodegaControlador {

    // El permiso se controla a nivel de modulo (permisos 6000-6003).
    private static final String RECURSO = "configuracion-crud";

    @Inject
    private BodegaServicio bodegaServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("descri") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("descri") String descri,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        BodegaFiltroDTO filtro = new BodegaFiltroDTO(descri, estado);

        List<BodegaDTO> resultado = bodegaServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = bodegaServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(bodegaServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = bodegaServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid BodegaDTO bodegaDTO) {
        BodegaDTO creada = bodegaServicio.crear(bodegaDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creada, "Bodega creada correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid BodegaDTO bodegaDTO) {
        BodegaDTO modificada = bodegaServicio.modificar(id, bodegaDTO);
        return Response.ok(RespuestaApi.exito(modificada, "Bodega modificada correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        bodegaServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Bodega eliminada correctamente")).build();
    }
}
