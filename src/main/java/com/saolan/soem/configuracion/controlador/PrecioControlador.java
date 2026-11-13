package com.saolan.soem.configuracion.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.configuracion.dto.PrecioDTO;
import com.saolan.soem.configuracion.dto.PrecioFiltroDTO;
import com.saolan.soem.configuracion.servicio.PrecioServicio;

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
 * Endpoints REST del catalogo PRECIO (listas de precio). El combo lo
 * consumen la asignacion por sucursal ({@code /sucursales/{id}/precios}),
 * roles-precios y los modulos de inventario, compras y ventas.
 */
@Path("/precios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PrecioControlador {

    // El permiso se controla a nivel de modulo (permisos 6000-6003).
    private static final String RECURSO = "configuracion-crud";

    @Inject
    private PrecioServicio precioServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("descri") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("codigo") String codigo,
            @QueryParam("descri") String descri,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        PrecioFiltroDTO filtro = new PrecioFiltroDTO(codigo, descri, estado);

        List<PrecioDTO> resultado = precioServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = precioServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(precioServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = precioServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid PrecioDTO precioDTO) {
        PrecioDTO creado = precioServicio.crear(precioDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creado, "Lista de precio creada correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid PrecioDTO precioDTO) {
        PrecioDTO modificado = precioServicio.modificar(id, precioDTO);
        return Response.ok(RespuestaApi.exito(modificado, "Lista de precio modificada correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        precioServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Lista de precio eliminada correctamente")).build();
    }

    /** Reactiva una lista eliminada: mismo permiso que el borrado logico (igual que sucursales). */
    @PUT
    @Path("/{id}/activar")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response activar(@PathParam("id") Integer id) {
        PrecioDTO activado = precioServicio.activar(id);
        return Response.ok(RespuestaApi.exito(activado, "Lista de precio activada correctamente")).build();
    }
}
